package org.patonki.citygml.math;

import org.apache.commons.math3.geometry.euclidean.threed.Plane;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.patonki.citygml.features.Point;

import java.util.Arrays;

public class MyPlane {
    private final Plane plane;
    private Point computeCenter(Point[] points) {
        double xSum, ySum, zSum;
        xSum = ySum = zSum = 0;
        for (Point point : points) {
            xSum += point.x();
            ySum += point.y();
            zSum += point.z();
        }
        return new Point(xSum / points.length, ySum / points.length, zSum / points.length);
    }
    //https://math.stackexchange.com/questions/99299/best-fitting-plane-given-a-set-of-points
    public MyPlane(Point[] points) {
        Point centric = computeCenter(points);
        Point[] mappedPoints = Arrays.stream(points)
                .map(p -> new Point(p.x() - centric.x(), p.y() - centric.y(), p.z() - centric.z()))
                .toArray(Point[]::new);
        double[][] matrixData = {
                Arrays.stream(mappedPoints).mapToDouble(Point::x).toArray(),
                Arrays.stream(mappedPoints).mapToDouble(Point::y).toArray(),
                Arrays.stream(mappedPoints).mapToDouble(Point::z).toArray()
        };
        SingularValueDecomposition lol = new SingularValueDecomposition(MatrixUtils.createRealMatrix(matrixData));
        RealMatrix u = lol.getU();

        double a = u.getEntry(0,2);
        double b = u.getEntry(1,2);
        double c = u.getEntry(2, 2);

        this.plane = new Plane(new Vector3D(centric.x(), centric.y(), centric.z()), new Vector3D(a,b,c), 0.001);
    }
    public Point project(Point p) {
        Vector3D projected = (Vector3D) this.plane.project(new Vector3D(p.x(), p.y(), p.z()));
        return new Point(projected.getX(), projected.getY(), projected.getZ());
    }
}
