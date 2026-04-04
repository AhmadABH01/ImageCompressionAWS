package quadtree;

import java.awt.Color;


public class AVL{

   private AVL filsG;
   private AVL filsD;
   private boolean estVide;
   private int balance=0;
   private Color couleur; 

public AVL(){
        this.filsD=null;
        this.filsG=null;
        this.couleur=null;
        this.estVide=true;
        

}
public AVL construireDepuisRQT(RQuadTree noeud, AVL avl) {
    if (noeud == null) {
        return avl;
    }
    
    // Si c'est une feuille, ajouter sa couleur
    if (noeud.estFeuille()) {
        // System.out.println("est une feuilee");
      
        Color c = noeud.getColor();
        //   System.out.println("Red");
        //   System.err.println(c.getRed());
        //   System.out.println("Green");
        //   System.out.println(c.getGreen());
        //   System.out.println("Blue");
        // System.out.println(c.getBlue());

        if (c != null) {
            avl = ajouter(avl, c).getAVL();
        }
    }
    // Sinon, parcourir les enfants
    else {
        avl = construireDepuisRQT(noeud.getNW(), avl);  // NO
        avl = construireDepuisRQT(noeud.getNE(), avl);  // NE
        avl = construireDepuisRQT(noeud.getSE(), avl);  // SE
        avl = construireDepuisRQT(noeud.getSW(), avl);  // SO
        
    }
    
    return avl;
}
  
public AVL(RQuadTree qtree) {
   AVL res = construireDepuisRQT(qtree, new AVL());
   this.filsG = res.filsG;
   this.filsD = res.filsD;
   this.couleur = res.couleur;
   this.estVide = res.estVide;
   this.balance = res.balance;
}
// J'AI MODIFIE ICI , J'AI ENLEVE FHEIGHT 
public AVL(ImagePNG imagePNG, int x, int y, int fWidth) {

    RQuadTree r = new  RQuadTree(imagePNG,x,y,fWidth);
    AVL avl = new AVL(r);
    this.filsG=avl.filsG;
    this.filsD=avl.filsD;
    this.couleur=avl.getColor();
    this.balance=avl.getBalance();
    this.estVide=false;
   }


public AVL(Color c, AVL g , AVL d){
             this.couleur=c;
             this.filsG=g;
             this.filsD=d;
             this.estVide=false;
}
public Color getColor() { return couleur; }

public AVL getFilsG() { return filsG; }

public AVL getFilsD() { return filsD; }

public int getBalance() { return balance; }

public boolean estVide() { return estVide; }

public Color getCouleur(){
    return couleur;
}
public Color getminCouleur() {
    if (this.getFilsG().estVide()) {
        return this.couleur;
    } else {
        return this.getFilsG().getminCouleur();
    }
}


// utilise ces fonctions pour calculer le balance
public int maximum(int x , int y,int  z){
int max=x;
    if(max<=y){max=y;}
    if(max<=z){max=z;}
    
        return max;  
}

public int minimum(int x , int y, int z){
int min=x;
    if(min>=y){min=y;}
    if(min>=z){min=z;}
    
        return min;  
}
//fonctionne

//parcours symetrique
public String toString(AVL avl){
    if(avl == null || avl.estVide()){
        return "";
    }
    return "(" + toString(avl.getFilsG())
           + ImagePNG.colorToHex(avl.getColor())
           + toString(avl.getFilsD()) + ")";
}

//fonctionne
 public static  AVL rechercher(Color c,AVL avl){
     if(avl==null){
         return null;
     }
     else{
         if(avl.getDirection(c)==0){
             return avl;
         }
         if(avl.getDirection(c)==1){
             return rechercher(c,avl.getFilsD());

         }
         else{
             return rechercher(c,avl.getFilsG());
         }

    }
}

public Result supprimer(Color c, AVL avl){
    int h;

    if(avl.estVide()){
        return new Result(0, avl);
    }
    else{
        // ----- CAS 1 : aller à droite -----
        if(avl.getDirection(c) == 1){
            Result res = supprimer(c, avl.getFilsD());
            avl.filsD = res.getAVL();
            h = res.getHeight();
        }

        // ----- CAS 2 : aller à gauche -----
        else if(avl.getDirection(c) == -1){
            Result res = supprimer(c, avl.getFilsG());
            avl.filsG = res.getAVL();
            h = -res.getHeight();
        }

        // ----- CAS 3 : c == avl.couleur : supprimer ce nœud -----
        else {
            // Sous-arbre gauche vide → on remplace par le fils droit
            if(avl.filsG.estVide()){
                return new Result(-1, avl.getFilsD());
            }

            // Sous-arbre droit vide → on remplace par le fils gauche
            if(avl.filsD.estVide()){
                return new Result(-1, avl.getFilsG());
            }

            // ----- CAS : les deux fils existent -----
            // On remplace la couleur du nœud par le min du sous-arbre droit
            avl.couleur = avl.getFilsD().getminCouleur();

            // Puis on supprime ce minimum
            Result res = supprMin(avl.getFilsD());
            avl.filsD = res.getAVL();
            h = res.getHeight();
        }

        // ----- Si aucune modification de hauteur -----
        if(h == 0){
            return new Result(0, avl);
        }

        // ----- Mise à jour du facteur d'équilibre -----
        avl.balance = avl.balance + h;

        // ----- Rééquilibrage -----
        avl = avl.equilibreAvl(avl);

        // ----- Détermination de la nouvelle variation de hauteur -----
        if(avl.getBalance() == 0){
            return new Result(-1, avl);
        }
        else{
            return new Result(0, avl);
        }
    }
}

//fonctionne
public AVL ROTG(AVL avl){
    System.out.println("");
    AVL B= avl.getFilsD();
    AVL A=avl;
    int a = A.getBalance();
    int b= B.getBalance();
    A.filsD= B.getFilsG();
    B.filsG= A;
    A.balance= a-maximum(b,0,0)-1;
    B.balance= minimum(a-2,a+b-2,b-1);

    return B;

}
//fonctionne
public AVL ROTD(AVL avl){
    
    AVL B= avl.getFilsG();
    AVL A=avl;
    int a = A.getBalance();
    int b= B.getBalance();
    A.filsG= B.getFilsD();
    B.filsD= A;
    A.balance= a-minimum(b,0,0)+1;
    B.balance= maximum(a+2,a+b+2,b+1);

    return B;

}
//fonctionnne
public AVL equilibreAvl(AVL avl){
    if(avl.balance==2){
        if(avl.getFilsD().getBalance()>=0){
            return ROTG (avl);

        }
        else{
            avl.filsD= ROTD(avl.filsD);
            return ROTG(avl);
        }
    }
    else{
        if(avl.balance==-2){
            if(avl.getFilsG().getBalance()<=0){
               return ROTD(avl);
            }
            else{
                 avl.filsG= ROTG(avl.filsG);
                 return ROTD(avl);
            }
        }
    }
    return avl;
}
public Result supprMin(AVL avl){
    int h;

    // ---- CAS : le fils gauche est vide → le min est ici ----
    if(avl.getFilsG().estVide()){
        // On retourne le fils droit, et la hauteur diminue de 1
        return new Result(-1, avl.getFilsD());
    }

    // ---- Sinon : on descend à gauche ----
    Result res = supprMin(avl.getFilsG());
    avl.filsG = res.getAVL();
    h = -res.getHeight();

    // ---- Si aucun changement de hauteur ----
    if(h == 0){
        return new Result(0, avl);
    }

    // ---- Mise à jour de bal ----
    avl.balance = avl.balance + h;

    // ---- Rééquilibrage ----
    avl = avl.equilibreAvl(avl);

    // ---- Mise à jour de la variation de hauteur ----
    if(avl.balance == 0){
        return new Result(-1, avl);
    }
    else{
        return new Result(0, avl);
    }
}

//Trouver la direction ou aller 1 pour aller a droite -1 a gauche et 0 si egale a lui meme
public int  getDirection(Color c) {
    int r1 = couleur.getRed();
    int g1 = couleur.getGreen();
    int b1 = couleur.getBlue();

    int r2 = c.getRed();
    int g2 = c.getGreen();
    int b2 = c.getBlue();

    if (r1 > r2) return -1;
    if (r1 < r2) return 1;

    if (g1 > g2) return -1;
    if (g1 < g2) return 1;

    if (b1 > b2) return -1;
    if (b1 < b2) return 1;

    // Si les trois composantes sont égales, on peut renvoyer l’un des deux
    return 0;
}

//Equilibre marche ajoute marche
public  Result ajouter(AVL avl,Color c){
        int h;
        if(avl.estVide()){
             return new Result(1,new AVL(c,new AVL(),new AVL()));
        }
        else{
            int d=avl.getDirection(c);
            if (d==0) {
                return new Result(0, avl);
            }
            if (d==1) {
                Result res = ajouter(avl.filsD,c);
                avl.filsD=res.getAVL();
                h=res.getHeight();
            }
            else{
                 Result res = ajouter(avl.filsG,c);
                avl.filsG=res.getAVL();
                h=-res.getHeight();
            }
            if(h==0){
               return new Result(0, avl);
            }
            else{
                 avl.balance= avl.balance + h;
                 AVL navl = equilibreAvl(avl);
                 if(navl.balance==0){
                    return new Result(0,navl);
                 }
                 else{
                    return new Result (1,navl);
                 }
            }

        }
            
}





}