/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.jdo.api.persistence.enhancer.classfile;

/**
 * InsnUtils provides a set of static methods which serve to
 * select vm instructions during code annotation.
 */
public class InsnUtils implements VMConstants {

    /**
     * Return the best instruction for loading the specified integer
     * constant onto the stack - hopefully use short form
     */
    public static Insn integerConstant(int i, ConstantPool pool) {
        if (i == 0)
            return Insn.create(opc_iconst_0);
        else if (i == 1)
            return Insn.create(opc_iconst_1);
        else if (i == 2)
            return Insn.create(opc_iconst_2);
        else if (i == 3)
            return Insn.create(opc_iconst_3);
        else if (i == 4)
            return Insn.create(opc_iconst_4);
        else if (i == 5)
            return Insn.create(opc_iconst_5);
        else if (i >= -128 && i < 128)
            return Insn.create(opc_bipush, i);
        return Insn.create(opc_ldc, pool.addInteger(i));
    }

    /**
     * Return the best instruction for loading the specified long constant onto
     * the stack.
     */
    public static Insn longConstant(long l, ConstantPool pool) {
        if (l == 0)
            return Insn.create(opc_lconst_0);
        else if (l == 1)
            return Insn.create(opc_lconst_1);
        else
            return Insn.create(opc_ldc2_w, pool.addLong(l));
    }

    /**
     * Return the best instruction for loading the specified float constant onto
     * the stack.
     */
    public static Insn floatConstant(float f, ConstantPool pool) {
        if (f == 0)
            return Insn.create(opc_fconst_0);
        else if (f == 1)
            return Insn.create(opc_fconst_1);
        else if (f == 2)
            return Insn.create(opc_fconst_2);
        else
            return Insn.create(opc_ldc, pool.addFloat(f));
    }

    /**
     * Return the best instruction for loading the specified double constant onto
     * the stack.
     */
    public static Insn doubleConstant(double d, ConstantPool pool) {
        if (d == 0)
            return Insn.create(opc_dconst_0);
        else if (d == 1)
            return Insn.create(opc_dconst_1);
        else
            return Insn.create(opc_ldc2_w, pool.addDouble(d));
    }

    /**
     * Return the best instruction for storing a reference to a local
     * variable slot
     */
    public static Insn aStore(int i, ConstantPool pool) {
        if (i == 0)
            return Insn.create(opc_astore_0);
        else if (i == 1)
            return Insn.create(opc_astore_1);
        else if (i == 2)
            return Insn.create(opc_astore_2);
        else if (i == 3)
            return Insn.create(opc_astore_3);
        return Insn.create(opc_astore, i);
    }

    /**
     * Return the best instruction for storing an int to a local
     * variable slot
     */
    public static Insn iStore(int i, ConstantPool pool) {
        if (i == 0)
            return Insn.create(opc_istore_0);
        else if (i == 1)
            return Insn.create(opc_istore_1);
        else if (i == 2)
            return Insn.create(opc_istore_2);
        else if (i == 3)
            return Insn.create(opc_istore_3);
        return Insn.create(opc_istore, i);
    }

    /**
     * Return the best instruction for storing a float to a local
     * variable slot
     */
    public static Insn fStore(int i, ConstantPool pool) {
        if (i == 0)
            return Insn.create(opc_fstore_0);
        else if (i == 1)
            return Insn.create(opc_fstore_1);
        else if (i == 2)
            return Insn.create(opc_fstore_2);
        else if (i == 3)
            return Insn.create(opc_fstore_3);
        return Insn.create(opc_fstore, i);
    }

    /**
     * Return the best instruction for storing a long to a local
     * variable slot
     */
    public static Insn lStore(int i, ConstantPool pool) {
        if (i == 0)
            return Insn.create(opc_lstore_0);
        else if (i == 1)
            return Insn.create(opc_lstore_1);
        else if (i == 2)
            return Insn.create(opc_lstore_2);
        else if (i == 3)
            return Insn.create(opc_lstore_3);
        return Insn.create(opc_lstore, i);
    }

    /**
     * Return the best instruction for storing a double to a local
     * variable slot
     */
    public static Insn dStore(int i, ConstantPool pool) {
        if (i == 0)
            return Insn.create(opc_dstore_0);
        else if (i == 1)
            return Insn.create(opc_dstore_1);
        else if (i == 2)
            return Insn.create(opc_dstore_2);
        else if (i == 3)
            return Insn.create(opc_dstore_3);
        return Insn.create(opc_dstore, i);
    }

    /**
     * Return the best instruction for loading a reference from a local
     * variable slot
     */
    public static Insn aLoad(int i, ConstantPool pool) {
        if (i == 0)
            return Insn.create(opc_aload_0);
        else if (i == 1)
            return Insn.create(opc_aload_1);
        else if (i == 2)
            return Insn.create(opc_aload_2);
        else if (i == 3)
            return Insn.create(opc_aload_3);
        return Insn.create(opc_aload, i);
    }

    /**
     * Return the best instruction for loading an int from a local
     * variable slot
     */
    public static Insn iLoad(int i, ConstantPool pool) {
        if (i == 0)
            return Insn.create(opc_iload_0);
        else if (i == 1)
            return Insn.create(opc_iload_1);
        else if (i == 2)
            return Insn.create(opc_iload_2);
        else if (i == 3)
            return Insn.create(opc_iload_3);
        return Insn.create(opc_iload, i);
    }

    /**
     * Return the best instruction for loading a float from a local
     * variable slot
     */
    public static Insn fLoad(int i, ConstantPool pool) {
        if (i == 0)
            return Insn.create(opc_fload_0);
        else if (i == 1)
            return Insn.create(opc_fload_1);
        else if (i == 2)
            return Insn.create(opc_fload_2);
        else if (i == 3)
            return Insn.create(opc_fload_3);
        return Insn.create(opc_fload, i);
    }

    /**
     * Return the best instruction for loading a long from a local
     * variable slot
     */
    public static Insn lLoad(int i, ConstantPool pool) {
        if (i == 0)
            return Insn.create(opc_lload_0);
        else if (i == 1)
            return Insn.create(opc_lload_1);
        else if (i == 2)
            return Insn.create(opc_lload_2);
        else if (i == 3)
            return Insn.create(opc_lload_3);
        return Insn.create(opc_lload, i);
    }

    /**
     * Return the best instruction for loading a double from a local
     * variable slot
     */
    public static Insn dLoad(int i, ConstantPool pool) {
        if (i == 0)
            return Insn.create(opc_dload_0);
        else if (i == 1)
            return Insn.create(opc_dload_1);
        else if (i == 2)
            return Insn.create(opc_dload_2);
        else if (i == 3)
            return Insn.create(opc_dload_3);
        return Insn.create(opc_dload, i);
    }

    /**
     * Return the best instruction for loading a value from a local
     * variable slot
     */
    public static Insn load(int tp, int i, ConstantPool pool) {
        switch(tp) {
            //@olsen: added these cases:
            case T_BOOLEAN:
            case T_CHAR:
            case T_BYTE:
            case T_SHORT:
                //@olsen: end added cases
            case T_INT:
                return iLoad(i, pool);
            case T_FLOAT:
                return fLoad(i, pool);
            case T_DOUBLE:
                return dLoad(i, pool);
            case T_LONG:
                return lLoad(i, pool);
            case TC_OBJECT:
                return aLoad(i, pool);
            default:
                throw new InsnError("bad load type");//NOI18N
        }
    }


    /**
     * Return the best instruction for storing a value to a local
     * variable slot
     */
    public static Insn store(int tp, int i, ConstantPool pool) {
        switch(tp) {
            //@olsen: added these cases:
            case T_BOOLEAN:
            case T_CHAR:
            case T_BYTE:
            case T_SHORT:
                //@olsen: end added cases
            case T_INT:
                return iStore(i, pool);
            case T_FLOAT:
                return fStore(i, pool);
            case T_DOUBLE:
                return dStore(i, pool);
            case T_LONG:
                return lStore(i, pool);
            case TC_OBJECT:
                return aStore(i, pool);
            default:
                throw new InsnError("bad store type");//NOI18N
        }
    }
}

