package quadtree;

import java.util.ArrayList;

public class AvlLamda {
    private AvlLamda filsG;
    private AvlLamda filsD;
    private boolean estVide;
    private int balance=0;
    private double lamda;
    private ArrayList<RQuadTree> surFeuilleLambda;

    public AvlLamda() {
        this.filsG=null;
        this.filsD=null;
        this.estVide=true;
        this.lamda=0.0;
        balance=0;
        surFeuilleLambda=new ArrayList<>();
    }

    public AvlLamda(double lamda,AvlLamda filsG, AvlLamda filsD) {
        this.filsG = filsG;
        this.filsD = filsD;
        this.lamda = lamda;
        balance=0;
        surFeuilleLambda=new ArrayList<>();
    }

    
    public AvlLamda getFilsG() {
        return filsG;
    }

    public void setFilsG(AvlLamda filsG) {
        this.filsG = filsG;
    }

    public AvlLamda getFilsD() {
        return this.filsD;
    }

    public void setFilsD(AvlLamda filsD) {
        this.filsD = filsD;
    }

    public boolean isEstVide() {
        return estVide;
    }
    public boolean setEstVide(boolean estVide) {
        return this.estVide = estVide;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }
    public double getLamda() {
        return lamda;
    }

    public void setLamda(double lamda) {
        this.lamda = lamda;
    }
    public ArrayList<RQuadTree> getSurFeuilleLambda() {
        return surFeuilleLambda;
    }

    public void setSurFeuilleLambda(ArrayList<RQuadTree> surFeuilleLambda) {
        this.surFeuilleLambda = surFeuilleLambda;
    }




    public int getBalance() {
        return this.balance;
    }

    public int hateur (AvlLamda avl) {
        if (avl.getFilsG() == null && avl.getFilsD() == null)
            return -1;
        else
        {
            int hateurGauch = hateur(avl.getFilsG());
            int hateurDroite = hateur( avl.getFilsD());
            return Math.max(hateurGauch, hateurDroite)+1;
        }
    }
    public AvlLamda ROTG (AvlLamda A) {
        AvlLamda B;
        int a,b;
        B=A.getFilsD();
        a= A.getBalance();
        b= B.getBalance();
        A.setFilsD(B.getFilsG());
        B.setFilsG(A);
        A.setBalance(a-Math.max(b,0)-1);
        B.setBalance(Math.min(a-2,Math.min(a+b-2,b-1)));
        return B;
    }

    public AvlLamda ROTD (AvlLamda A) {
        AvlLamda B;
        int a,b;
        B=A.getFilsG();
        a= A.getBalance();
        b= B.getBalance();
        A.setFilsG(B.getFilsD());
        B.setFilsD(A);
        A.setBalance(a-Math.min(b,0)+1);
        B.setBalance(Math.max(a+2,Math.max(a+b+2,b+1)));
        return B;
    }

    public AvlLamda equilibreAvl(AvlLamda avl){
        if(avl.getBalance()==2){
            if(avl.getFilsD().getBalance()>=0){
                return ROTG(avl);

            }
            else{
                avl.setFilsD(ROTD(avl.getFilsD()));
                return ROTG(avl);
            }
        }
        else if(avl.getBalance()==-2){
                if(avl.getFilsG().getBalance()<=0){
                    return ROTD(avl);
                }
                else{
                    avl.setFilsG(ROTG(avl.getFilsG()));
                    return ROTD(avl);
                }
            } else return avl;
        }

       public Result addLamda(AvlLamda avl,double lamda,RQuadTree rQuadTree){
           int hauteur;
           if(avl.isEstVide()){
               AvlLamda newNode = new AvlLamda(lamda, new AvlLamda(), new AvlLamda());
               newNode.setEstVide(false);
               newNode.getSurFeuilleLambda().add(rQuadTree);
               return new Result(1, newNode);
           }
           else if (lamda== avl.getLamda()){
               avl.getSurFeuilleLambda().add(rQuadTree);
               return new Result(0, avl);
           }
        else if (lamda>avl.getLamda()){
            Result result1 = addLamda(avl.getFilsD(),lamda,rQuadTree);
            avl.setFilsD(result1.getAvlamda());
            hauteur=result1.getHeight();
        }
        else
        {
            Result res = addLamda(avl.getFilsG(),lamda,rQuadTree);
            avl.setFilsG(res.getAvlamda());
            hauteur=-res.getHeight();
        }
        if (hauteur==0)
            return new Result(0,avl);
        else
        {
            avl.setBalance(avl.getBalance()+hauteur);
            AvlLamda tempAvl =equilibreAvl(avl);
            if(tempAvl.getBalance()==0){
                return new Result(0,tempAvl);
            }
            else{
                return new Result (1,tempAvl);
            }

        }
       }

    public Result removeMin(AvlLamda avl) {
        if (avl.isEstVide()) {
            return new Result(0, avl,null);
        }

        if (avl.getFilsG().isEstVide()) {
            ArrayList<RQuadTree> liste = avl.getSurFeuilleLambda();
            if (!liste.isEmpty()) {
                RQuadTree removed = liste.remove(0);

                if (!liste.isEmpty()) {
                    return new Result(0, avl, removed);
                }

                return new Result(-1, avl.getFilsD(), removed); // ← RETURN IT
            }

            return new Result(0, avl,null);
        }

        else {
            Result res = removeMin(avl.getFilsG());
            avl.setFilsG(res.getAvlamda());
            int hauteur = -res.getHeight();

            if (hauteur == 0) {
                return new Result(0, avl,res.getRemovedQuad());
            }
            else {
                avl.setBalance(avl.getBalance() + hauteur);
                AvlLamda tempAvl = equilibreAvl(avl);

                if (tempAvl.getBalance() == 0) {
                    return new Result(-1, tempAvl,res.getRemovedQuad());
                }
                else {
                    return new Result(0, tempAvl,res.getRemovedQuad());
                }
            }
        }
    }

}


