/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.report.projectinfo.dependencies.renderer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes small jars with real class files, so the jar analysis behind the dependencies report runs on them.
 */
final class JarFixtures {

    private JarFixtures() {}

    /** A jar entry: a class file of the given class file major version, or plain bytes. */
    static final class Entry {
        final String name;
        final byte[] content;

        private Entry(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }

        /** A class file; {@code internalName} is e.g. {@code p/Foo}, {@code major} 52 is Java 8. */
        static Entry classFile(String path, String internalName, int major) throws IOException {
            return new Entry(path, classBytes(internalName, major, false, false));
        }

        /** A class file whose one method has a line number table, which the jar analysis reports as debug information. */
        static Entry classFileWithDebugInfo(String path, String internalName, int major) throws IOException {
            return new Entry(path, classBytes(internalName, major, false, true));
        }

        /** A {@code module-info.class}. */
        static Entry moduleInfo(String path, int major) throws IOException {
            return new Entry(path, classBytes("module-info", major, true, false));
        }

        static Entry manifest(String... lines) {
            StringBuilder sb = new StringBuilder("Manifest-Version: 1.0\r\n");
            for (String line : lines) {
                sb.append(line).append("\r\n");
            }
            return new Entry("META-INF/MANIFEST.MF", sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        static Entry resource(String path) {
            return new Entry(path, new byte[] {1, 2, 3});
        }
    }

    /**
     * Writes a jar of exactly {@code length} bytes: the entries plus a stored padding entry sized to fit.
     */
    static File jar(File dir, String fileName, long length, Entry... entries) throws IOException {
        File file = new File(dir, fileName);
        write(file, 0, entries);
        long base = file.length();
        if (base > length) {
            throw new IllegalArgumentException("a jar of the entries alone is " + base + " bytes, more than " + length);
        }
        write(file, (int) (length - base), entries);
        if (file.length() != length) {
            throw new IllegalStateException("padding did not give " + length + " bytes but " + file.length());
        }
        return file;
    }

    private static void write(File file, int padding, Entry... entries) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            for (Entry entry : entries) {
                ZipEntry zipEntry = new ZipEntry(entry.name);
                zipEntry.setTime(0);
                zip.putNextEntry(zipEntry);
                zip.write(entry.content);
                zip.closeEntry();
            }
            byte[] pad = new byte[padding];
            CRC32 crc = new CRC32();
            crc.update(pad);
            ZipEntry stored = new ZipEntry("pad.bin");
            stored.setTime(0);
            stored.setMethod(ZipEntry.STORED);
            stored.setSize(pad.length);
            stored.setCompressedSize(pad.length);
            stored.setCrc(crc.getValue());
            zip.putNextEntry(stored);
            zip.write(pad);
            zip.closeEntry();
        }
    }

    /** The smallest valid class file: no fields, no methods, only a name and {@code java/lang/Object}. */
    private static byte[] classBytes(String internalName, int major, boolean module, boolean debug) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(0xCAFEBABE);
        out.writeShort(0);
        out.writeShort(major);
        if (module) {
            // 1 Utf8 module-info, 2 Class, 3 Utf8 Module, 4 Utf8 m, 5 Module
            out.writeShort(6);
            out.writeByte(1);
            out.writeUTF("module-info");
            out.writeByte(7);
            out.writeShort(1);
            out.writeByte(1);
            out.writeUTF("Module");
            out.writeByte(1);
            out.writeUTF("m");
            out.writeByte(19);
            out.writeShort(4);
            out.writeShort(0x8000); // ACC_MODULE
            out.writeShort(2); // this_class
            out.writeShort(0); // super_class
            out.writeShort(0); // interfaces
            out.writeShort(0); // fields
            out.writeShort(0); // methods
            out.writeShort(1); // attributes: Module
            out.writeShort(3);
            out.writeInt(16);
            out.writeShort(5); // module_name_index
            out.writeShort(0); // flags
            out.writeShort(0); // version
            out.writeShort(0); // requires
            out.writeShort(0); // exports
            out.writeShort(0); // opens
            out.writeShort(0); // uses
            out.writeShort(0); // provides
        } else {
            // 1 Utf8 name, 2 Class, 3 Utf8 java/lang/Object, 4 Class
            // with debug information: 5 Utf8 m, 6 Utf8 ()V, 7 Utf8 Code, 8 Utf8 LineNumberTable
            out.writeShort(debug ? 9 : 5);
            out.writeByte(1);
            out.writeUTF(internalName);
            out.writeByte(7);
            out.writeShort(1);
            out.writeByte(1);
            out.writeUTF("java/lang/Object");
            out.writeByte(7);
            out.writeShort(3);
            if (debug) {
                for (String utf8 : new String[] {"m", "()V", "Code", "LineNumberTable"}) {
                    out.writeByte(1);
                    out.writeUTF(utf8);
                }
            }
            out.writeShort(0x0021); // public super
            out.writeShort(2); // this_class
            out.writeShort(4); // super_class
            out.writeShort(0); // interfaces
            out.writeShort(0); // fields
            if (debug) {
                out.writeShort(1); // methods
                out.writeShort(0x0009); // public static
                out.writeShort(5); // name
                out.writeShort(6); // descriptor
                out.writeShort(1); // attributes: Code
                out.writeShort(7);
                out.writeInt(25);
                out.writeShort(0); // max_stack
                out.writeShort(0); // max_locals
                out.writeInt(1); // code_length
                out.writeByte(0xB1); // return
                out.writeShort(0); // exception table
                out.writeShort(1); // attributes: LineNumberTable
                out.writeShort(8);
                out.writeInt(6);
                out.writeShort(1); // one entry
                out.writeShort(0); // start_pc
                out.writeShort(1); // line_number
            } else {
                out.writeShort(0); // methods
            }
            out.writeShort(0); // attributes
        }
        out.flush();
        return bytes.toByteArray();
    }
}
