# Image Compression Service on AWS

A serverless image compression service using R-quadtree algorithms, deployed on AWS with Lambda, S3, SQS, and SNS.

**Authors:** Ahmad ABU HANNUD & Burul ASSEFA
**Course:** Algorithmique & Structures de données 3 (2025/2026)

---

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│  S3 Input   │────▶│     SQS     │────▶│   Lambda    │
│  (Upload)   │     │   Bucket    │     │    Queue    │     │ (Compress)  │
└─────────────┘     └─────────────┘     └─────────────┘     └──────┬──────┘
                                                                    │
                    ┌─────────────┐     ┌─────────────┐             │
                    │     SNS     │◀────│  S3 Output  │◀────────────┘
                    │   (Email)   │     │   Bucket    │
                    └─────────────┘     └─────────────┘
```

**AWS Services Used:**
- **S3** - Input/output storage for images
- **SQS** - Message queue for event-driven processing
- **Lambda** - Serverless compute for compression
- **SNS** - Email notifications on completion
- **API Gateway** - REST API endpoint

---

## Project Structure

```
CompressionImageAWS/
├── src/
│   ├── main/java/
│   │   ├── handler/
│   │   │   ├── LambdaHandler.java    # API Gateway handler
│   │   │   └── SqsHandler.java       # SQS event processor
│   │   ├── service/
│   │   │   └── CompressionService.java  # Core compression logic
│   │   └── quadtree/
│   │       ├── RQuadTree.java        # R-quadtree implementation
│   │       ├── AVL.java              # AVL tree for colors
│   │       ├── AvlLamda.java         # AVL for Lambda compression
│   │       ├── ImagePNG.java         # PNG image utilities
│   │       └── Main.java             # Local CLI (optional)
│   └── teraform/
│       └── main.tf                   # Infrastructure as Code
├── pom.xml                           # Maven configuration
└── README.md
```

---

## Compression Algorithms

### Lambda Compression (Quality-controlled)
Controls image quality by merging quadtree nodes with color variance below threshold λ (0-255).
- **Lower λ** = Higher quality, larger file
- **Higher λ** = Lower quality, smaller file
- **Complexity:** O(N)

### Phi Compression (Size-controlled)
Controls output size by limiting the number of quadtree nodes to φ.
- **Higher φ** = More nodes, higher quality
- **Lower φ** = Fewer nodes, smaller file
- **Complexity:** O(N log N)

---

## Prerequisites

- Java 11+
- Maven 3.6+
- AWS CLI configured with credentials
- Terraform 1.0+

---

## Build

```bash
# Build the JAR with all dependencies
mvn clean package
```

This creates `target/image-compression-lambda-1.0.jar`.

---

## Deploy to AWS

```bash
# Navigate to Terraform directory
cd src/teraform

# Initialize Terraform
terraform init

# Preview changes
terraform plan

# Deploy infrastructure
terraform apply
```

**Outputs after deployment:**
- `api_url` - REST API endpoint
- `input_bucket` - S3 bucket for uploads
- `output_bucket` - S3 bucket for compressed images
- `sqs_queue_url` - SQS queue URL
- `sns_topic_arn` - SNS topic for notifications

---

## Usage

### Option 1: Automatic Processing (S3 Upload)

Upload a PNG image to the input bucket and receive the compressed result automatically:

```bash
# Upload with default settings (lambda method, threshold 30)
aws s3 cp /Users/ahmadabuhannud/Desktop/L3-INFO/TpAsd3/projet-asd3-rquadtree/CompressionImageAWS/src/256-tux.png s3://image-compression-input-3a8a19c0/256-tux.png

# Upload with custom compression parameters
aws s3 cp /Users/ahmadabuhannud/Desktop/L3-INFO/TpAsd3/projet-asd3-rquadtree/CompressionImageAWS/src/256-tux.png \
  s3://image-compression-input-3a8a19c0/256-tux.png \
  --metadata method=lambda,threshold=50
```

**Supported metadata:**
- `method` - `lambda` or `phi` (default: `lambda`)
- `threshold` - Integer value (default: `30`)

The compressed image will appear in the output bucket as `my-image-compressed.png`, and you'll receive an email notification with EQM (quality metric) and processing time.

### Option 2: REST API

```bash
curl -X POST "https://mzcnrbtvod.execute-api.eu-west-3.amazonaws.com/prod/compress" \
  -H "Content-Type: application/json" \
  -d '{
    "inputPath": "s3://image-compression-input-3a8a19c0/256-tux.png",
    "outputPath": "s3://image-compression-output-3a8a19c0/256-tux-compressed.png",
    "method": "lambda",
    "threshold": 50
  }'

```

**Response:**
```json
{
  "success": true,
  "message": "Compression successful",
  "outputPath": "s3://output-bucket/compressed.png",
  "eqm": 12.45,
  "executionTimeMs": 1234
}
```

---

## Algorithm Complexity

| Operation | Complexity |
|-----------|------------|
| Lambda Compression | O(N) |
| Phi Compression | O(N log N) |
| AVL Insert/Delete | O(log K) |

Where N = number of pixels, K = number of colors in AVL tree.

---

## Test Images

Sample images are included in `src/`:
- `2.png`, `4.png`, `8.png`, `16.png` - Small test images
- `32-tux.png`, `64-tuxette.png` - Medium images
- `128-gnu.png`, `256-tux.png`, `256-trash.png` - Larger images
- `512-books.png`, `1024-cube.png` - Large test images

---

## Cleanup

To destroy all AWS resources:

```bash
cd src/teraform
terraform destroy
```

## using terminal 
javac -source 1.8 -target 1.8 -d bin src/main/java/quadtree/*.java 
java -classpath bin quadtree.Main ./src/256-tux.png lambda 50
ls -lh src/256-tux.png /tmp/256-tux-compressed.png


# urls 
api_url = "https://mzcnrbtvod.execute-api.eu-west-3.amazonaws.com/prod/compress"
input_bucket = "image-compression-input-3a8a19c0"
output_bucket = "image-compression-output-3a8a19c0"
sns_topic_arn = "arn:aws:sns:eu-west-3:313162186353:image-compression-notifications"
sqs_queue_url = "https://sqs.eu-west-3.amazonaws.com/313162186353/image-compression-queue"


### version for S3 without trigger 
# Get the UUID of the event source mapping
aws lambda list-event-source-mappings --function-name image-compression-sqs

# Disable it
aws lambda update-event-source-mapping --uuid <5c7fd985-00db-46b0-a29c-f38c41fb33c0> --enabled false

# Upload your image (won't be processed)
aws s3 cp image.png s3://image-compression-input-3a8a19c0/image.png

# Re-enable when ready
aws lambda update-event-source-mapping --uuid <UUID> --enabled true

# delete from the S3 
# Input bucket
aws s3 rm s3://image-compression-input-3a8a19c0/ --recursive

# Output bucket
aws s3 rm s3://image-compression-output-3a8a19c0/ --recursive
