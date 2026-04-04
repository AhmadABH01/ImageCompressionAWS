package quadtree;

import java.awt.*;
import java.util.ArrayList;

public class RQuadTree {

    // changement
    private static int nbFeuilles=0;
    private RQuadTree parent;
    private RQuadTree PositionR[] = new RQuadTree[4];
    private ImagePNG image;
    private int x,y;
    private Color color;
    private int size;
    public RQuadTree(ImagePNG imagePNG, int x, int y, int size) {
        if (imagePNG.width() != imagePNG.height() || (imagePNG.width() & (imagePNG.width() - 1)) != 0) {
            throw new IllegalArgumentException("L'image doit être carrée de taille 2^n");
        }
        this.image = imagePNG;
        this.x = x;
        this.y = y;
        this.size = size;
        // cas de base : une seule pixel
        if (size == 1) {

            this.color = imagePNG.getPixel(x, y);
            PositionR[0] = PositionR[1] = PositionR[2] = PositionR[3] = null;
            RQuadTree.nbFeuilles++;

        }
        // cas récursif : diviser en 4 sous-régions
        else if (size>= 2) {
            //changement ajoute parent pour compressPhi
            PositionR[0] = new RQuadTree(imagePNG, x, y, size/2); // NO
            PositionR[0].parent = this;
            PositionR[1] = new RQuadTree(imagePNG, x + size/2, y, size/2); // NE
            PositionR[1].parent = this;
            PositionR[2] = new RQuadTree(imagePNG, x, y + size/2, size/2); // SO
            PositionR[2].parent = this;
            PositionR[3] = new RQuadTree(imagePNG, x + size/2, y + size/2, size/2);// SE
            PositionR[3].parent = this;

            // Vérifier si toutes les sous-régions sont des feuilles avec la même couleur
                   // on verifie que toutes les sous-régions ont une couleur définie (non nulle)
                   // car si une sous-région n'est pas une feuille, sa couleur sera nulle
                boolean tousOntCouleur = (this.PositionR[0].color != null &&
                         this.PositionR[1].color != null &&
                         this.PositionR[2].color != null &&
                         this.PositionR[3].color != null);

                   // Puis vérifier que toutes les couleurs sont égales
                   boolean tousMemeCouleur = tousOntCouleur &&
                         this.PositionR[0].color.equals(this.PositionR[1].color) &&
                         this.PositionR[0].color.equals(this.PositionR[2].color) &&
                         this.PositionR[0].color.equals(this.PositionR[3].color);
                // Si oui, fusionner en une feuille
                if (tousMemeCouleur)
                {
                   this.color = this.PositionR[0].color;
                    PositionR[0] = null;
                    PositionR[1] = null;
                    PositionR[2] = null;
                    PositionR[3] = null;

                }
                // Sinon, ce n'est pas une feuille

        }
    }




    public boolean estFeuille() {
        return this.PositionR[0] == null && this.PositionR[1] == null && this.PositionR[2] == null && this.PositionR[3] == null;
    }

    private boolean isSurFeuille() {

        if (this.estFeuille()) {
            return false;
        }

        return  this.PositionR[0] != null && this.PositionR[0].estFeuille() &&
                this.PositionR[1] != null && this.PositionR[1].estFeuille() &&
                this.PositionR[2] != null && this.PositionR[2].estFeuille()&&
                this.PositionR[3] != null && this.PositionR[3].estFeuille();
    }

    public RQuadTree getParent() { return this.parent; }
// des fonctions pour la compression par lambda et phi
     // Calculer la couleur moyenne des quatre sous-régions
private Color colorMoyenne() {
    Color NO = this.PositionR[0].color;
    Color NE = this.PositionR[1].color;
    Color SW = this.PositionR[2].color;
    Color SE = this.PositionR[3].color;
    int MoyenRouge = (NO.getRed() + NE.getRed() + SW.getRed() + SE.getRed()) / 4;
    int MoyenVerte = (NO.getGreen() + NE.getGreen() + SW.getGreen() + SE.getGreen()) / 4;
    int MoyenBlue = (NO.getBlue() + NE.getBlue() + SW.getBlue() + SE.getBlue()) / 4;

    return new Color(MoyenRouge, MoyenVerte, MoyenBlue);
}
    // Calculer la luminance d'une couleur
    private double getLuminance(Color c) {
        return 0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue();
    }
    // Calculer la dégradation maximale de luminance par rapport à la couleur moyenne
    private double degradationLuminance(double luminanceMoyenne) {
        double deg0 = Math.abs(luminanceMoyenne - getLuminance(this.PositionR[0].color));
        double deg1 = Math.abs(luminanceMoyenne - getLuminance(this.PositionR[1].color));
        double deg2 = Math.abs(luminanceMoyenne - getLuminance(this.PositionR[2].color));
        double deg3 = Math.abs(luminanceMoyenne - getLuminance(this.PositionR[3].color));

        return Math.max(Math.max(deg0, deg1), Math.max(deg2, deg3));
    }
   //
   public void compressLambda(int lambda) {

       if (lambda < 0 || lambda > 255) {
           return;
       }

       if (this.estFeuille()) {
           return;
       }

       this.PositionR[0].compressLambda(lambda);
       this.PositionR[1].compressLambda(lambda);
       this.PositionR[2].compressLambda(lambda);
       this.PositionR[3].compressLambda(lambda);

       if (this.isSurFeuille()) {
           Color colorMoyenne = this.colorMoyenne();
           double luminanceMoyenne = getLuminance(colorMoyenne);
           double degradation = degradationLuminance(luminanceMoyenne);


           if (degradation <= lambda) {
               this.PositionR[0] = null;
               this.PositionR[1] = null;
               this.PositionR[2] = null;
               this.PositionR[3] = null;
               this.color = colorMoyenne;
               return;
           }
       }
       else
       {
           this.PositionR[0].compressLambda(lambda);
           this.PositionR[1].compressLambda(lambda);
           this.PositionR[2].compressLambda(lambda);
           this.PositionR[3].compressLambda(lambda);
       }



   }

   // fonction pour obtenir la valeur de lambda d'une région , ca nous aidera dans la compression par phi
   private double getLambda()
   {
       Color ColorMoyenne = this.colorMoyenne();
       double LuminanceMoyeenne = getLuminance(ColorMoyenne);
       double degradationLuminance =  degradationLuminance(LuminanceMoyeenne);
       return degradationLuminance;
   }

   // obtenir la liste des régions sur-feuilles
   public ArrayList<RQuadTree> getSurFeuille()
   {
       ArrayList<RQuadTree> NbsurFeuille = new ArrayList<>();
       getSurFeuilleRecursive(this, NbsurFeuille);
       return NbsurFeuille;
   }
   // fonction récursive pour obtenir les régions sur-feuilles
   private void getSurFeuilleRecursive(RQuadTree region , ArrayList<RQuadTree> SurFeuille)
   {
       if (region == null || region.estFeuille()) {

           return;
       }
       if (region.isSurFeuille())
           {
               SurFeuille.add(region);
           }
       else
           {
            getSurFeuilleRecursive(region.PositionR[0], SurFeuille);
            getSurFeuilleRecursive(region.PositionR[1], SurFeuille);
            getSurFeuilleRecursive(region.PositionR[2], SurFeuille);
            getSurFeuilleRecursive(region.PositionR[3], SurFeuille);

           }

   }

    public void compressPhi(int phi) {
        ArrayList<RQuadTree> liste = this.getSurFeuille();
        AvlLamda arbre = new AvlLamda();

        for (RQuadTree sf : liste) {

            Result res = arbre.addLamda(arbre, sf.getLambda(), sf);
            arbre = res.getAvlamda();
        }
        //changment de conditions
        while (RQuadTree.nbFeuilles > phi) {

            if (liste.isEmpty()) break;


            Result minResult = arbre.removeMin(arbre);
            RQuadTree removedQuad = minResult.getRemovedQuad();

            if (removedQuad == null) break;

            RQuadTree parentQ = removedQuad.getParent();

            Color avg = removedQuad.colorMoyenne();
            removedQuad.PositionR[0] = null;
            removedQuad.PositionR[1] = null;
            removedQuad.PositionR[2] = null;
            removedQuad.PositionR[3] = null;
            removedQuad.color = avg;

            RQuadTree.nbFeuilles -= 3;
            if (parentQ != null && !parentQ.estFeuille() && parentQ.isSurFeuille()) {
                double newLambda = parentQ.getLambda();
                Result newRes = arbre.addLamda(arbre, newLambda, parentQ);
                arbre = newRes.getAvlamda();
            }
        }
    }
    @Override
    public String toString() {
        if (this.estFeuille()) {
            // C'est une feuille  - juste retourner la colour
            return ImagePNG.colorToHex(this.image.getPixel(this.x, this.y));
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            sb.append(PositionR[0].toString());
            sb.append(" ");
            sb.append(PositionR[1].toString());
            sb.append(" ");
            sb.append(PositionR[2].toString());
            sb.append(" ");
            sb.append(PositionR[3].toString());
            sb.append(")");
            return sb.toString();
        }
    }

    /**
     * Génère une image PNG à partir du quadtree compressé
     * @return Une nouvelle ImagePNG représentant l'arbre
     */
    public ImagePNG toPNG() {
        // Créer une nouvelle image de la même taille que l'originale
        ImagePNG resultImage = image.clone();

        // Remplir l'image en parcourant le quadtree
        fillImage(resultImage);

        return resultImage;
    }

    /**
     * Méthode récursive pour remplir l'image
     * @param img L'image à remplir
     */
    private void fillImage(ImagePNG img) {
        // Cas 1: C'est une feuille - remplir toute la région avec sa couleur
        if (this.estFeuille()) {
            // Remplir tous les pixels de cette région
            for (int i = 0; i < this.size; i++) {
                for (int j = 0; j < this.size; j++) {
                   
                    img.setPixel(this.x + i, this.y + j, this.color);
                }
            }
        }
        // Cas 2: C'est un nœud interne - récurser sur les 4 enfants
        else {
            if (this.PositionR[0] != null) {
                this.PositionR[0].fillImage(img);
            }
            if (this.PositionR[1] != null) {
                this.PositionR[1].fillImage(img);
            }
            if (this.PositionR[2] != null) {
                this.PositionR[2].fillImage(img);
            }
            if (this.PositionR[3] != null) {
                this.PositionR[3].fillImage(img);
            }
        }
    }

    /**
     * Vérifie si ce nœud est une feuille
     * @return true si c'est une feuille, false sinon
     */
    public Color getColor() {
        return this.color;
    }

    public RQuadTree getNE() {
        return this.PositionR[1];
    }

    public RQuadTree getNW() {
        return this.PositionR[0];
    }
    public RQuadTree getSW() {
        return this.PositionR[2];
    }
    public RQuadTree getSE() {
        return this.PositionR[3];
    }
}


