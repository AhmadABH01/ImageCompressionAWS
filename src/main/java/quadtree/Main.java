package quadtree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.awt.Color;

public class Main {

    private static RQuadTree currentQuadtree = null;
    private static Scanner scanner = new Scanner(System.in);
    private static AVL currentAVL = null;
    private static ImagePNG currentImage = null;
    public static void main(String[] args) {
        
        if (args.length == 3) {
            runNonInteractiveMode(args[0], args[1], Integer.parseInt(args[2]));
            return;
        }

        
        System.out.println("--- Projet Compression d'images Bitmap ---");
        
        while (true) {
            displayMenu();
            
            try {
                System.out.print("Entrez votre choix: ");
                int choice = scanner.nextInt();
                scanner.nextLine(); 

                if (choice == 0) {
                    System.out.println("Programme terminé. Au revoir! ");
                    break; 
                }
                
                processChoice(choice);
                
            } catch (java.util.InputMismatchException e) {
                System.out.println(" Entrée invalide. Veuillez entrer un numéro.");
                scanner.nextLine(); 
            } catch (Exception e) {
                System.err.println(" Une erreur inattendue est survenue: " + e.getMessage());
            }
        }
        scanner.close();
    }

    private static void displayMenu() {
        System.out.println("\n----------------------------------------");
        System.out.println("Menu Principal (R-Quadtree & Compression):");
        System.out.println("1. Construire le R-quadtree");
        System.out.println("2. Appliquer une compression Lambda");
        System.out.println("3. Appliquer une compression Phi");
        System.out.println("4. Sauvegarder l'image compressée (PNG)");
        System.out.println("5. Sauvegarder la représentation textuelle (TXT)");
        System.out.println("6. Construire le AVL des couleur en utilisant un image donné ");
        System.out.println("7. Construire le AVL des couleur en utilisant un RQuadtree  ");
        System.out.println("8. Sauvegarder la représentation textuelle (TXT) d'un AVL des couleurs ");
        System.out.println("9. Effecteur les operation , ajoute , supprimer , rechercher sur un AVL des couleurs ");
        System.out.println("10. Avoir la valeur de EQM d'un RQuadtree (non implémenté)");
        System.out.println("0. Quitter");
        System.out.println("----------------------------------------");}

    private static void processChoice(int choice) {
        switch (choice) {
            case 1:
                buildRQuadtree();
                break;
            case 2:
                applyCompressionLambda();
                break;
            case 3:
                applyCompressionPhi();
                break;
            case 4:
                saveImagePNG();
                break;
            case 5:
                saveRQuadtreeText();
                break;
            case 6:
                buildAVLFromImage();
                break; 
            case 7:
                buildAVLFromRQuadtree();
                break;
            case 8:
                saveAVLText();
                break;
            case 9:
               AvlOperations();
                break;
            case 10:
                EQM();
                break;
            default:
                System.out.println("Choix non reconnu. Veuillez choisir un numéro valide.");
        }
    }


    private static void buildRQuadtree() {
        System.out.print("Nom du fichier PNG: ");
        String fileName = scanner.nextLine();
        try {
            currentImage = new ImagePNG(fileName);
            currentQuadtree = new RQuadTree(currentImage, 0, 0, currentImage.width()); 
            System.out.println(" R-quadtree construit pour " + fileName + ".");
        } catch (IOException e) {
            System.err.println(" Erreur de lecture du fichier: " + e.getMessage());
        }
    }

    private static void applyCompressionLambda() {
        if (!checkQuadtreeReady()) return;
        System.out.print("Valeur Lambda (0-255): ");
        try {
            int lambda = scanner.nextInt(); 
            scanner.nextLine();
            if (lambda >= 0 && lambda <= 255) {
                currentQuadtree.compressLambda(lambda); 
                System.out.println("Compression Lambda terminée.");
            } else {
                System.out.println(" Lambda doit être entre 0 et 255.");
            }
        } catch (java.util.InputMismatchException e) {
            System.out.println(" Veuillez entrer un nombre valide.");
            scanner.nextLine();
        }
    }

    private static void applyCompressionPhi() {
        if (!checkQuadtreeReady()) return;
        System.out.print("Valeur Phi (nombre max de feuilles): ");
        try {
            int phi = scanner.nextInt();
            scanner.nextLine();
            if (phi > 0) {
                currentQuadtree.compressPhi(phi); 
                System.out.println(" Compression Phi terminée.");
            } else {
                System.out.println(" Phi doit être supérieur à 0.");
            }
        } catch (java.util.InputMismatchException e) {
            System.out.println(" Veuillez entrer un nombre entier valide.");
            scanner.nextLine();
        }
    }

    private static void saveImagePNG() {
        if (!checkQuadtreeReady()) return;
        System.out.print("Nom du fichier PNG de sortie: ");
        String outFileName = scanner.nextLine();
        try {
            ImagePNG compressedImage = currentQuadtree.toPNG(); 
            compressedImage.save(outFileName+".png");
            System.out.println("Image sauvegardée dans " + outFileName);
        } catch (IOException e) {
            System.err.println(" Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    private static void saveRQuadtreeText() {
        if (!checkQuadtreeReady()) return;
        System.out.print("Nom du fichier TXT de sortie: ");
        String outFileName = scanner.nextLine();
        try {
            String treeText = currentQuadtree.toString();
            saveTextToFile(treeText, outFileName); 
            System.out.println(" Représentation textuelle sauvegardée dans " + outFileName);
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }
    private static void EQM() {
        if (!checkQuadtreeReady()) return;
        System.out.println(" La valeur de EQM est : " + ImagePNG.computeEQM(currentImage, currentQuadtree.toPNG()));

    }
    private static boolean checkQuadtreeReady() {
        if (currentQuadtree == null) {
            System.out.println(" Veuillez d'abord construire le R-quadtree (Option 1).");
            return false;
        }
        return true;
    }
    
    private static void saveTextToFile(String content, String fileName) throws IOException {
        java.io.FileWriter writer = new java.io.FileWriter(fileName);
        writer.write(content);
        writer.close();
    }
    private static void buildAVLFromImage() {
        // Placeholder for AVL construction from image
        
        System.out.print("Nom du fichier PNG: ");
        String fileName = scanner.nextLine();
        try {
            ImagePNG image = new ImagePNG(fileName);
            currentAVL = new AVL(image, 0, 0, image.width()); 
            System.out.println(" avl construit pour " + fileName + ".");
        } catch (IOException e) {
            System.err.println(" Erreur de lecture du fichier: " + e.getMessage());
        }

    }
    private static void buildAVLFromRQuadtree() {
        // Placeholder for AVL construction from R-Quadtree
        if (!checkQuadtreeReady()) return;
        currentAVL = new AVL(currentQuadtree); 
        System.out.println(" avl construit pour le currentQuadtree.");
    }
    private static void saveAVLText() {
        if (currentAVL == null) {
            System.out.println(" Veuillez d'abord construire l'AVL des couleurs.");
            return;
        }
        System.out.print("Nom du fichier TXT de sortie: ");
        String outFileName = scanner.nextLine();
        try {
            String avlText = currentAVL.toString(currentAVL);
            saveTextToFile(avlText, outFileName); 
            System.out.println(" Représentation textuelle de l'AVL sauvegardée dans " + outFileName);
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }
    
    private static void AvlOperations() {
        if (currentAVL == null) {
            System.out.println(" Veuillez d'abord construire l'AVL des couleurs.");
            return;
        }
        System.out.println("Opérations sur l'AVL des couleurs:");
        System.out.println("1. Ajouter une couleur");
        System.out.println("2. Supprimer une couleur");
        System.out.println("3. Rechercher une couleur");
        System.out.print("Choisissez une opération: ");
        int opChoice = scanner.nextInt();
        scanner.nextLine(); 

        System.out.print("Entrez la couleur au format hex (ex: #RRGGBB): ");
        String colorHex = scanner.nextLine();
        Color color = ImagePNG.hexToColor(colorHex);
        Result currentAVLrslt;
        switch (opChoice) {
            case 1:
                 currentAVLrslt = currentAVL.ajouter(currentAVL,color);
                 currentAVL = currentAVLrslt.getAVL();
                System.out.println("Couleur ajoutée: " + colorHex);
                break;
            case 2:
                 currentAVLrslt = currentAVL.supprimer(color,currentAVL);
                currentAVL = currentAVLrslt.getAVL();               
                System.out.println("Couleur supprimée: " + colorHex);
                break;
            case 3:
                AVL foundNode = AVL.rechercher(color, currentAVL);
                if (foundNode != null) {
                    System.out.println("Couleur trouvée: " + colorHex);
                } else {
                    System.out.println("Couleur non trouvée: " + colorHex);
                }
                break;
            default:
                System.out.println("Opération non reconnue.");
        }
    }
private static void runNonInteractiveMode(String fileName, String method, int param) {


        // Chargement de l'image PNG
         
        try {
            ImagePNG img = new ImagePNG(fileName);
        

        // Création du Quadtree à partir de cette image
         currentQuadtree = new RQuadTree(img,0,0,img.width());

        //Application de la compression à qualité controlée par lambda.
        if (method.equals("lambda")) {
            currentQuadtree.compressLambda(param);
        }
        //Application de la compression à poids controlée par phi.
        else if (method.equals("phi")) {
            currentQuadtree.compressPhi(param);
        }
        // Exception si la méthode n'est pas reconnue
        else {
            throw new IllegalArgumentException("Méthode de compression inconnue : " + method);
        }


        // Sauvgarder les fichiers de sortie
        String nomFichierEntree = fileName.substring(0,fileName.lastIndexOf('.'));
        String ImageSortie = nomFichierEntree +"-"+ method + param +".png";
        String TexteSortie = nomFichierEntree +"-"+ method + param +".txt";

        // Génération et sauvegarde de l'image compressée
        ImagePNG imgCompressee = currentQuadtree.toPNG();
        imgCompressee.save(ImageSortie);

        // Sauvegarde du texte représentant le quadtree
        Files.writeString(Path.of(TexteSortie), currentQuadtree.toString());
    }
     catch (IOException e) {
            e.printStackTrace();
        }

    }
}                   

