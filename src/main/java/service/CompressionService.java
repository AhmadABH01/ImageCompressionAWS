// ce class est utilise par aws lambda pour le compression des images
package service;

import quadtree.ImagePNG;
import quadtree.RQuadTree;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;

public class CompressionService {

    private static final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

    public static class CompressionResult
    {
        public boolean success;
        public String message;
        public String outputPath; 
        public double eqm;
        public long executionTimeMs;
        public CompressionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public static CompressionResult compress(String inputPath, String outputPath, String method, int threshold) 
    {
        long startTime = System.currentTimeMillis();

        try {
            //Parse S3 paths (format: "s3://bucket/key")
            String inputBucket = inputPath.replace("s3://", "").split("/")[0];
            String inputKey = inputPath.replace("s3://" + inputBucket + "/", "");
            String outputBucket = outputPath.replace("s3://", "").split("/")[0];
            String outputKey = outputPath.replace("s3://" + outputBucket + "/", "");

            // Download image from S3
            S3Object s3Object = s3Client.getObject(inputBucket, inputKey);
            InputStream inputStream = s3Object.getObjectContent();
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            inputStream.close();

            // Use BufferedImage constructor instead of file path
            ImagePNG img = new ImagePNG(bufferedImage);
            ImagePNG originalImg = new ImagePNG(bufferedImage);
            
            
            RQuadTree currentQuadtree = new RQuadTree(img, 0, 0, img.width());

            if (method.equals("lambda")) {
                if(threshold < 0 || threshold > 255)
                    return new CompressionResult(false, "Lambda Must be between 0 and 255");
                currentQuadtree.compressLambda(threshold);
            }
            else if (method.equals("phi")) {
                if(threshold <= 0)
                    return new CompressionResult(false, "Phi Must be greater than 0");
                currentQuadtree.compressPhi(threshold);
            }
            else {
                return new CompressionResult(false, "Unknown method: " + method + ". Use 'lambda' or 'phi'");
            }

            ImagePNG compressedImage = currentQuadtree.toPNG();

            // Save to temp file, then upload to S3
            File tempFile = File.createTempFile("compressed", ".png");
            compressedImage.save(tempFile.getAbsolutePath());
            s3Client.putObject(outputBucket, outputKey, tempFile);
            tempFile.delete();

            double eqm = ImagePNG.computeEQM(originalImg, compressedImage);
            
            CompressionResult result = new CompressionResult(true, "Compression successful");
            result.outputPath = outputPath;
            result.eqm = eqm;
            result.executionTimeMs = System.currentTimeMillis() - startTime;  //was wrong before
            return result;
        }
        catch(IOException e)
        {
            return new CompressionResult(false, "IO Error: " + e.getMessage());
        }
        catch(Exception e)
        {
            return new CompressionResult(false, "Error: " + e.getMessage());
        }
    }
}