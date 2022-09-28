// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package edu.wpi.first.math.geometry;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.Vector;
import edu.wpi.first.math.numbers.N3;

/** A class representing a coordinate system axis within the NWU coordinate system. */
public class CoordinateAxis {
    final Vector<N3> m_axis;

    /**
     * Constructs a coordinate system axis within the NWU coordinate system and normalizes it.
     *
     * @param x The x component.
     * @param y The y component.
     * @param z The z component.
     */
    public CoordinateAxis(double x, double y, double z) {
        double norm = Math.sqrt(x * x + y * y + z * z);
        m_axis = VecBuilder.fill(x / norm, y / norm, z / norm);
    }

    /**
     * Returns a coordinate axis corresponding to +X in the NWU coordinate system.
     *
     * @return A coordinate axis corresponding to +X in the NWU coordinate system.
     */
    @SuppressWarnings("MethodName")
    public static CoordinateAxis N() {
        return new CoordinateAxis(1.0, 0.0, 0.0);
    }

    /**
     * Returns a coordinate axis corresponding to -X in the NWU coordinate system.
     *
     * @return A coordinate axis corresponding to -X in the NWU coordinate system.
     */
    @SuppressWarnings("MethodName")
    public static CoordinateAxis S() {
        return new CoordinateAxis(-1.0, 0.0, 0.0);
    }

    /**
     * Returns a coordinate axis corresponding to -Y in the NWU coordinate system.
     *
     * @return A coordinate axis corresponding to -Y in the NWU coordinate system.
     */
    @SuppressWarnings("MethodName")
    public static CoordinateAxis E() {
        return new CoordinateAxis(0.0, -1.0, 0.0);
    }

    /**
     * Returns a coordinate axis corresponding to +Y in the NWU coordinate system.
     *
     * @return A coordinate axis corresponding to +Y in the NWU coordinate system.
     */
    @SuppressWarnings("MethodName")
    public static CoordinateAxis W() {
        return new CoordinateAxis(0.0, 1.0, 0.0);
    }

    /**
     * Returns a coordinate axis corresponding to +Z in the NWU coordinate system.
     *
     * @return A coordinate axis corresponding to +Z in the NWU coordinate system.
     */
    @SuppressWarnings("MethodName")
    public static CoordinateAxis U() {
        return new CoordinateAxis(0.0, 0.0, 1.0);
    }

    /**
     * Returns a coordinate axis corresponding to -Z in the NWU coordinate system.
     *
     * @return A coordinate axis corresponding to -Z in the NWU coordinate system.
     */
    @SuppressWarnings("MethodName")
    public static CoordinateAxis D() {
        return new CoordinateAxis(0.0, 0.0, -1.0);
    }
}
