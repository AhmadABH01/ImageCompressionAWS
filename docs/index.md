# Image Compression Service on AWS

Serverless image compression service using **R-quadtree algorithms**, deployed
on AWS with Lambda, S3, SQS, and SNS.

**Authors:** Ahmad ABU HANNUD &amp; Burul ASSEFA
**Course:** Algorithmique &amp; Structures de données 3 (2025/2026)

---

## Architecture

```text
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

## Compression Algorithms

### Lambda Compression (Quality-controlled)

Controls image quality by merging quadtree nodes with color variance below
threshold &lambda; (0-255).

- **Lower &lambda;** = Higher quality, larger file
- **Higher &lambda;** = Lower quality, smaller file
- **Complexity:** O(N)

### Phi Compression (Size-controlled)

Controls output size by limiting the number of quadtree nodes to &phi;.

- **Higher &phi;** = More nodes, higher quality
- **Lower &phi;** = Fewer nodes, smaller file
- **Complexity:** O(N log N)

---

## Code Layout

| Package / file                              | Role                                 |
|---------------------------------------------|--------------------------------------|
| `handler/LambdaHandler.java`                | API Gateway entry point              |
| `handler/SqsHandler.java`                   | SQS event processor                  |
| `service/CompressionService.java`           | Compression orchestration            |
| `quadtree/RQuadTree.java`                   | R-quadtree implementation            |
| `quadtree/AVL.java`                         | AVL tree of colors                   |
| `quadtree/AvlLamda.java`                    | AVL keyed by lambda variance         |
| `quadtree/ImagePNG.java`                    | PNG IO utilities                     |
| `quadtree/Result.java`                      | Recursion result value object        |
| `quadtree/Main.java`                        | Local CLI                            |
| `src/teraform/main.tf`                      | Terraform IaC                        |

---

## Build &amp; Deploy

```bash
# Build the JAR
mvn clean package

# Deploy infrastructure
cd src/teraform
terraform init
terraform apply
```

After `terraform apply`, the outputs include:

- `api_url` - REST API endpoint
- `input_bucket` / `output_bucket` - S3 bucket names
- `sqs_queue_url` - SQS queue URL
- `sns_topic_arn` - SNS topic for notifications

---

## Algorithm Complexity

| Operation           | Complexity |
|---------------------|------------|
| Lambda Compression  | O(N)       |
| Phi Compression     | O(N log N) |
| AVL Insert/Delete   | O(log K)   |

Where N = number of pixels, K = number of colors in the AVL tree.
