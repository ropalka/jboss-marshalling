/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.marshalling.river;

/**
 *
 */
public class ClassDescriptor {
    private final Class<?> type;
    private int typeID;

    public static final ClassDescriptor STRING_DESCRIPTOR = new ClassDescriptor(String.class, Protocol.ID_STRING_CLASS);
    public static final ClassDescriptor CLASS_DESCRIPTOR = new ClassDescriptor(Class.class, Protocol.ID_CLASS_CLASS);
    public static final ClassDescriptor OBJECT_DESCRIPTOR = new ClassDescriptor(Object.class, Protocol.ID_OBJECT_CLASS);
    public static final ClassDescriptor ENUM_DESCRIPTOR = new ClassDescriptor(Enum.class, Protocol.ID_ENUM_CLASS);

    public static final ClassDescriptor BOOLEAN = new ClassDescriptor(boolean.class, Protocol.ID_PRIM_BOOLEAN);
    public static final ClassDescriptor BYTE = new ClassDescriptor(byte.class, Protocol.ID_PRIM_BYTE);
    public static final ClassDescriptor SHORT = new ClassDescriptor(short.class, Protocol.ID_PRIM_SHORT);
    public static final ClassDescriptor INT = new ClassDescriptor(int.class, Protocol.ID_PRIM_INT);
    public static final ClassDescriptor LONG = new ClassDescriptor(long.class, Protocol.ID_PRIM_LONG);
    public static final ClassDescriptor CHAR = new ClassDescriptor(char.class, Protocol.ID_PRIM_CHAR);
    public static final ClassDescriptor FLOAT = new ClassDescriptor(float.class, Protocol.ID_PRIM_FLOAT);
    public static final ClassDescriptor DOUBLE = new ClassDescriptor(double.class, Protocol.ID_PRIM_DOUBLE);
    public static final ClassDescriptor VOID = new ClassDescriptor(void.class, Protocol.ID_VOID);

    public static final ClassDescriptor BOOLEAN_OBJ = new ClassDescriptor(Boolean.class, Protocol.ID_BOOLEAN_CLASS);
    public static final ClassDescriptor BYTE_OBJ = new ClassDescriptor(Byte.class, Protocol.ID_BYTE_CLASS);
    public static final ClassDescriptor SHORT_OBJ = new ClassDescriptor(Short.class, Protocol.ID_SHORT_CLASS);
    public static final ClassDescriptor INTEGER_OBJ = new ClassDescriptor(Integer.class, Protocol.ID_INTEGER_CLASS);
    public static final ClassDescriptor LONG_OBJ = new ClassDescriptor(Long.class, Protocol.ID_LONG_CLASS);
    public static final ClassDescriptor CHARACTER_OBJ = new ClassDescriptor(Character.class, Protocol.ID_CHARACTER_CLASS);
    public static final ClassDescriptor FLOAT_OBJ = new ClassDescriptor(Float.class, Protocol.ID_FLOAT_CLASS);
    public static final ClassDescriptor DOUBLE_OBJ = new ClassDescriptor(Double.class, Protocol.ID_DOUBLE_CLASS);
    public static final ClassDescriptor VOID_OBJ = new ClassDescriptor(Void.class, Protocol.ID_VOID_CLASS);

    public static final ClassDescriptor BOOLEAN_ARRAY = new ClassDescriptor(boolean[].class, Protocol.ID_BOOLEAN_ARRAY_CLASS);
    public static final ClassDescriptor BYTE_ARRAY = new ClassDescriptor(byte[].class, Protocol.ID_BYTE_ARRAY_CLASS);
    public static final ClassDescriptor SHORT_ARRAY = new ClassDescriptor(short[].class, Protocol.ID_SHORT_ARRAY_CLASS);
    public static final ClassDescriptor INT_ARRAY = new ClassDescriptor(int[].class, Protocol.ID_INT_ARRAY_CLASS);
    public static final ClassDescriptor LONG_ARRAY = new ClassDescriptor(long[].class, Protocol.ID_LONG_ARRAY_CLASS);
    public static final ClassDescriptor CHAR_ARRAY = new ClassDescriptor(char[].class, Protocol.ID_CHAR_ARRAY_CLASS);
    public static final ClassDescriptor FLOAT_ARRAY = new ClassDescriptor(float[].class, Protocol.ID_FLOAT_ARRAY_CLASS);
    public static final ClassDescriptor DOUBLE_ARRAY = new ClassDescriptor(double[].class, Protocol.ID_DOUBLE_ARRAY_CLASS);

    public ClassDescriptor(final Class<?> type, final int typeID) {
        this.type = type;
        this.typeID = typeID;
    }

    public Class<?> getType() {
        return type;
    }

    public int getTypeID() {
        return typeID;
    }

    public static ClassDescriptor forClass(Class<?> clazz) {
        return null;
    }
}