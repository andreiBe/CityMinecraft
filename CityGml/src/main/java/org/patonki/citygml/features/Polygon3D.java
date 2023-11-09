package org.patonki.citygml.features;

import org.patonki.citygml.math.MyPlane;
import org.patonki.citygml.math.Point2D;
import org.patonki.citygml.math.Vector;
import org.patonki.data.BoundingBox;
import org.patonki.data.BoundingBox3D;
import org.patonki.data.IntBoundingBox;

import java.util.ArrayList;
import java.util.Arrays;

public class Polygon3D extends Feature{
    public static class PolygonException extends Exception {
        public PolygonException(String msg) {
            super(msg);
        }
    }
    private final Texture texture;
    private double xmin, ymin, zmin;
    private Point[] points;

    private Vector v, u, o;

    public Polygon3D(Point[] points, Texture texture) throws PolygonException {
        this.texture = texture;
        //removing the last one
        this.points = new Point[points.length-1];
        System.arraycopy(points, 0, this.points, 0, this.points.length);

        ArrayList<Point> pts = new ArrayList<>();
        ArrayList<Integer> removedIndexes = new ArrayList<>();
        for (int i = 0; i < points.length; i++) {
            Point point = points[i];

            if (pts.stream().anyMatch(pt -> pt.distance(point) < 0.2)) {
                removedIndexes.add(i);
                continue;
            }

            pts.add(point);
        }
        if (pts.size() < 3) {
            throw new PolygonException("Polygon is too small!");
        }
        if (this.texture instanceof ImgTexture imgTexture) {
            imgTexture.removePoints(removedIndexes);
        }

        this.points = new Point[pts.size()];
        this.xmin = 999999999999d;
        this.ymin = 999999999999d;
        this.zmin = 999999999999d;
        for (Point v : pts) {
            xmin = Math.min(xmin, v.x());
            ymin = Math.min(ymin, v.y());
            zmin = Math.min(zmin, v.z());
        }
        this.points = pts.stream().map(p  -> new Point(p.x()-xmin, p.y()-ymin, p.z()-zmin)).toArray(Point[]::new);

        MyPlane plane = new MyPlane(this.points);

        this.points = Arrays.stream(this.points).map(plane::project).toArray(Point[]::new);
        initTo2D();
    }
    public Texture getTexture() {
        return texture;
    }
    private boolean intersects(Point2D A, Point2D B, Point2D P) {
        if (A.y() > B.y())
            return intersects(B, A, P);

        if (P.y() > B.y() || P.y() < A.y() || P.x() >= Math.max(A.x(), B.x()))
            return false;

        if (P.x() < Math.min(A.x(), B.x()))
            return true;

        double red = (P.y() - A.y()) / (P.x() - A.x());
        double blue = (B.y() - A.y()) / (B.x() - A.x());
        return red >= blue;
    }
    public Point2D pointTo2D(Point point) {
        Vector p = new Vector(point);
        try {
            return new Point2D(
                    p.subtract(o).dot(u),
                    p.subtract(o).dot(v)
            );
        } catch (IllegalArgumentException e) {
            System.out.println(u + " " + v);
            throw e;
        }
    }
    public Point pointTo3D(Point2D point) {
        Vector p = o.add(u.multiply(point.x())).add(v.multiply(point.y()));
        return p.add(new Vector(xmin, ymin, zmin));
    }
    public boolean contains(Point2D point2D) {
        boolean inside = false;
        for (int i = 0; i < points.length; i++) {
            Point2D p1 = pointTo2D(points[i]);
            Point2D p2 = pointTo2D(points[(i+1)%points.length]);
            if (intersects(p1,p2, point2D)) {
                inside = !inside;
            }
        }
        return inside;
    }

    //https://stackoverflow.com/questions/62475889/point-in-polygon-3d-same-plane-algorithm
    //https://dev.to/ndesmic/mapping-3d-points-to-2d-and-polygonal-centroids-hb9
    private void initTo2D() throws PolygonException {

        double bestDotProduct = 1;
        int[] bestDotProductIndexes = new int[3];
        for (int i = 0; i < points.length; i++) {
            for (int j = i+1; j < points.length; j++) {
                for (int k = j+1; k < points.length; k++) {
                    Vector vector1 = Vector.fromAtoB(points[i], points[j]).normalize();
                    Vector vector2 = Vector.fromAtoB(points[i], points[k]).normalize();
                    double dp = Math.abs(vector1.dot(vector2));
                    if (dp < bestDotProduct) {
                        bestDotProduct = dp;
                        bestDotProductIndexes = new int[] {i, j, k};
                    }
                }
            }
        }
        if (bestDotProduct == 1) {
            throw new PolygonException("Points do not form a plane!");
        }

        int i = bestDotProductIndexes[0];
        int j = bestDotProductIndexes[1];
        int k = bestDotProductIndexes[2];
        Vector vector1 = Vector.fromAtoB(points[i], points[j]);
        Vector vector2 = Vector.fromAtoB(points[i], points[k]);
        Vector n = vector1.cross(vector2).normalize();
        this.o = new Vector(points[0]);
        this.u = vector1.normalize();
        this.v = u.cross(n).normalize();
    }
    public BoundingBox getBBox2D() {
        double minX = 99999999999999d;
        double minY = 99999999999999d;
        double maxX = -999999999999999d;
        double maxY = -999999999999999d;
        for (Point point3D : points) {
            Point2D p = pointTo2D(point3D);
            minX = Math.min(minX, p.x());
            minY = Math.min(minY, p.y());
            maxX = Math.max(maxX, p.x());
            maxY = Math.max(maxY, p.y());
        }
        return new BoundingBox(minX, minY, maxX,maxY);
    }

    public IntBoundingBox getBBox2DVoxel() {
        BoundingBox bbox= getBBox2D();
        int x = (int) (bbox.minX());
        int y = (int) (bbox.minY());
        int maxX = (int) (bbox.maxX())+2;
        int maxY = (int) (bbox.maxY())+2;
        return new IntBoundingBox(x,y,maxX,maxY);
    }

    @Override
    public BoundingBox3D getBBox() {
        Point[] points = new Point[this.points.length];
        for (int i = 0; i < this.points.length; i++) {
            Point point = this.points[i];
            points[i] = new Point(point.x()+xmin, point.y()+ymin, point.z()+zmin);
        }
        return getBoundingBoxFromArray(points);
    }


    public int getPointAmount() {
        return this.points.length;
    }

    public Point2D[] getPoints2DCopy() {
        Point2D[] copy = new Point2D[this.points.length];
        for (int i = 0; i < this.points.length; i++) {
            copy[i] = pointTo2D(this.points[i]);
        }
        return copy;
    }


    @Override
    public int hashCode() {
        return Arrays.hashCode(points);
    }
}
