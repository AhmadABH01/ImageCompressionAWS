package quadtree;

public class Result {
    private int hauteur;
    private  AVL avl;
    private AvlLamda avlamda;
    private RQuadTree removedQuad;
    public Result(int h,AVL avl){
        this.hauteur=h;
        this.avl=avl;
    }
    public Result(int h,AvlLamda avl,RQuadTree removedQuad){
        this.hauteur=h;
        this.avlamda=avl;
        this.removedQuad=removedQuad;
    }

    public Result(int h,AvlLamda avl){
        this.hauteur=h;
        this.avlamda=avl;
    }

    public AVL getAVL(){
        return this.avl;
    }

    public int getHeight(){
        return hauteur;
    }

    public AvlLamda getAvlamda() {
        return avlamda;
    }

    public RQuadTree getRemovedQuad() {
        return removedQuad;
    }

}
