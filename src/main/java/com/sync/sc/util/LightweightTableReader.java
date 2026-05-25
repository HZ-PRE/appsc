package com.sync.sc.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class LightweightTableReader {
    private static final byte[] CFB_MAGIC = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
    private static final int END_OF_CHAIN = 0xFFFFFFFE;
    private static final int FREE_SECTOR = 0xFFFFFFFF;

    private LightweightTableReader() {
    }

    static List<List<String>> read(InputStream inputStream, String extension) throws IOException {
        if ("csv".equalsIgnoreCase(extension)) {
            return readCsv(inputStream);
        }
        if ("xls".equalsIgnoreCase(extension)) {
            return readXls(inputStream.readAllBytes());
        }
        throw new IOException("unsupported table type: " + extension);
    }

    private static List<List<String>> readCsv(InputStream inputStream) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                rows.add(parseCsvLine(line));
            }
        }
        return rows;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(ch);
            }
        }
        cells.add(cell.toString());
        return cells;
    }

    private static List<List<String>> readXls(byte[] fileBytes) throws IOException {
        if (fileBytes.length < CFB_MAGIC.length) {
            return List.of();
        }
        for (int i = 0; i < CFB_MAGIC.length; i++) {
            if (fileBytes[i] != CFB_MAGIC[i]) {
                throw new IOException("not a legacy .xls file");
            }
        }
        byte[] workbook = new CompoundFile(fileBytes).readStream("Workbook", "Book");
        if (workbook.length == 0) {
            return List.of();
        }
        return parseWorkbook(workbook);
    }

    private static List<List<String>> parseWorkbook(byte[] workbook) {
        List<String> sharedStrings = new ArrayList<>();
        Map<Integer, Map<Integer, String>> rows = new HashMap<>();
        int pos = 0;
        while (pos + 4 <= workbook.length) {
            int sid = u16(workbook, pos);
            int len = u16(workbook, pos + 2);
            int data = pos + 4;
            if (data + len > workbook.length) {
                break;
            }
            if (sid == 0x00FC) {
                sharedStrings = parseSst(workbook, data, len);
            } else if (sid == 0x00FD && len >= 10) {
                int row = u16(workbook, data);
                int col = u16(workbook, data + 2);
                int sstIndex = i32(workbook, data + 6);
                put(rows, row, col, sstIndex >= 0 && sstIndex < sharedStrings.size() ? sharedStrings.get(sstIndex) : "");
            } else if (sid == 0x0203 && len >= 14) {
                int row = u16(workbook, data);
                int col = u16(workbook, data + 2);
                double value = ByteBuffer.wrap(workbook, data + 6, 8).order(ByteOrder.LITTLE_ENDIAN).getDouble();
                put(rows, row, col, formatNumber(value));
            } else if (sid == 0x027E && len >= 10) {
                int row = u16(workbook, data);
                int col = u16(workbook, data + 2);
                put(rows, row, col, formatNumber(decodeRk(i32(workbook, data + 6))));
            } else if (sid == 0x0204 && len >= 8) {
                int row = u16(workbook, data);
                int col = u16(workbook, data + 2);
                int textLen = u16(workbook, data + 6);
                if (data + 8 + textLen <= data + len) {
                    put(rows, row, col, new String(workbook, data + 8, textLen, Charset.forName("GBK")));
                }
            }
            pos = data + len;
        }
        return toList(rows);
    }

    private static List<String> parseSst(byte[] workbook, int data, int len) {
        List<String> strings = new ArrayList<>();
        if (len < 8) {
            return strings;
        }
        int uniqueCount = i32(workbook, data + 4);
        int pos = data + 8;
        int end = data + len;
        for (int i = 0; i < uniqueCount && pos + 3 <= end; i++) {
            int cch = u16(workbook, pos);
            int flags = workbook[pos + 2] & 0xff;
            pos += 3;
            int richRuns = 0;
            int extSize = 0;
            if ((flags & 0x08) != 0 && pos + 2 <= end) {
                richRuns = u16(workbook, pos);
                pos += 2;
            }
            if ((flags & 0x04) != 0 && pos + 4 <= end) {
                extSize = i32(workbook, pos);
                pos += 4;
            }
            boolean utf16 = (flags & 0x01) != 0;
            int byteLen = cch * (utf16 ? 2 : 1);
            if (pos + byteLen > end) {
                break;
            }
            Charset charset = utf16 ? StandardCharsets.UTF_16LE : Charset.forName("Cp1252");
            strings.add(new String(workbook, pos, byteLen, charset));
            pos += byteLen + richRuns * 4 + extSize;
        }
        return strings;
    }

    private static void put(Map<Integer, Map<Integer, String>> rows, int row, int col, String value) {
        rows.computeIfAbsent(row, key -> new HashMap<>()).put(col, value);
    }

    private static List<List<String>> toList(Map<Integer, Map<Integer, String>> rows) {
        List<Integer> rowIndexes = rows.keySet().stream().sorted().toList();
        List<List<String>> result = new ArrayList<>(rowIndexes.size());
        for (Integer rowIndex : rowIndexes) {
            Map<Integer, String> row = rows.get(rowIndex);
            int maxCol = row.keySet().stream().max(Comparator.naturalOrder()).orElse(-1);
            List<String> values = new ArrayList<>(maxCol + 1);
            for (int col = 0; col <= maxCol; col++) {
                values.add(row.getOrDefault(col, ""));
            }
            result.add(values);
        }
        return result;
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static double decodeRk(int rk) {
        double value;
        if ((rk & 0x02) != 0) {
            value = rk >> 2;
        } else {
            long raw = ((long) rk & 0xFFFFFFFCL) << 32;
            value = Double.longBitsToDouble(raw);
        }
        return (rk & 0x01) != 0 ? value / 100.0 : value;
    }

    private static int u16(byte[] bytes, int pos) {
        return (bytes[pos] & 0xff) | ((bytes[pos + 1] & 0xff) << 8);
    }

    private static int i32(byte[] bytes, int pos) {
        return (bytes[pos] & 0xff)
                | ((bytes[pos + 1] & 0xff) << 8)
                | ((bytes[pos + 2] & 0xff) << 16)
                | ((bytes[pos + 3] & 0xff) << 24);
    }

    private static long u64(byte[] bytes, int pos) {
        return ((long) i32(bytes, pos) & 0xffffffffL) | (((long) i32(bytes, pos + 4) & 0xffffffffL) << 32);
    }

    private static final class CompoundFile {
        private final byte[] bytes;
        private final int sectorSize;
        private final int miniSectorSize;
        private final int[] fat;
        private final int firstDirSector;
        private final int firstMiniFatSector;
        private final int miniFatSectorCount;
        private final List<DirEntry> entries;
        private final DirEntry root;
        private final int[] miniFat;
        private final byte[] miniStream;

        CompoundFile(byte[] bytes) throws IOException {
            this.bytes = bytes;
            this.sectorSize = 1 << u16(bytes, 30);
            this.miniSectorSize = 1 << u16(bytes, 32);
            this.firstDirSector = i32(bytes, 48);
            this.firstMiniFatSector = i32(bytes, 60);
            this.miniFatSectorCount = i32(bytes, 64);
            this.fat = loadFat(i32(bytes, 44));
            this.entries = readDirectory();
            this.root = entries.stream().filter(e -> e.type == 5).findFirst().orElse(null);
            this.miniFat = loadMiniFat();
            this.miniStream = root == null ? new byte[0] : readRegularStream(root.startSector, (int) root.size);
        }

        byte[] readStream(String... names) throws IOException {
            for (String name : names) {
                for (DirEntry entry : entries) {
                    if (entry.type == 2 && name.equalsIgnoreCase(entry.name)) {
                        if (entry.size < 4096 && miniStream.length > 0) {
                            return readMiniStream(entry.startSector, (int) entry.size);
                        }
                        return readRegularStream(entry.startSector, (int) entry.size);
                    }
                }
            }
            return new byte[0];
        }

        private int[] loadFat(int fatSectorCount) {
            List<Integer> sectors = new ArrayList<>();
            for (int i = 0; i < 109; i++) {
                int sector = i32(bytes, 76 + i * 4);
                if (sector >= 0 && sector != FREE_SECTOR) {
                    sectors.add(sector);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int i = 0; i < Math.min(fatSectorCount, sectors.size()); i++) {
                out.writeBytes(sectorBytes(sectors.get(i)));
            }
            byte[] fatBytes = out.toByteArray();
            int[] result = new int[fatBytes.length / 4];
            for (int i = 0; i < result.length; i++) {
                result[i] = i32(fatBytes, i * 4);
            }
            return result;
        }

        private List<DirEntry> readDirectory() throws IOException {
            byte[] dirBytes = readRegularStream(firstDirSector, Integer.MAX_VALUE);
            List<DirEntry> result = new ArrayList<>();
            for (int pos = 0; pos + 128 <= dirBytes.length; pos += 128) {
                int nameLen = u16(dirBytes, pos + 64);
                if (nameLen < 2) {
                    continue;
                }
                String name = new String(dirBytes, pos, nameLen - 2, StandardCharsets.UTF_16LE);
                int type = dirBytes[pos + 66] & 0xff;
                int start = i32(dirBytes, pos + 116);
                long size = u64(dirBytes, pos + 120);
                result.add(new DirEntry(name, type, start, size));
            }
            return result;
        }

        private int[] loadMiniFat() throws IOException {
            if (firstMiniFatSector < 0 || miniFatSectorCount <= 0) {
                return new int[0];
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int sector = firstMiniFatSector;
            for (int i = 0; i < miniFatSectorCount && sector >= 0 && sector < fat.length; i++) {
                out.writeBytes(sectorBytes(sector));
                sector = fat[sector];
            }
            byte[] fatBytes = out.toByteArray();
            int[] result = new int[fatBytes.length / 4];
            for (int i = 0; i < result.length; i++) {
                result[i] = i32(fatBytes, i * 4);
            }
            return result;
        }

        private byte[] readRegularStream(int startSector, int sizeLimit) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int sector = startSector;
            int guard = 0;
            while (sector >= 0 && sector != END_OF_CHAIN && sector < fat.length && guard++ < fat.length) {
                out.writeBytes(sectorBytes(sector));
                if (out.size() >= sizeLimit) {
                    break;
                }
                sector = fat[sector];
            }
            byte[] data = out.toByteArray();
            return sizeLimit == Integer.MAX_VALUE || data.length <= sizeLimit ? data : java.util.Arrays.copyOf(data, sizeLimit);
        }

        private byte[] readMiniStream(int startSector, int sizeLimit) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int sector = startSector;
            int guard = 0;
            while (sector >= 0 && sector != END_OF_CHAIN && sector < miniFat.length && guard++ < miniFat.length) {
                int offset = sector * miniSectorSize;
                int len = Math.min(miniSectorSize, miniStream.length - offset);
                if (len <= 0) {
                    break;
                }
                out.write(miniStream, offset, len);
                if (out.size() >= sizeLimit) {
                    break;
                }
                sector = miniFat[sector];
            }
            byte[] data = out.toByteArray();
            return data.length <= sizeLimit ? data : java.util.Arrays.copyOf(data, sizeLimit);
        }

        private byte[] sectorBytes(int sector) {
            int offset = 512 + sector * sectorSize;
            int len = Math.min(sectorSize, bytes.length - offset);
            if (offset < 0 || len <= 0) {
                return new byte[0];
            }
            return java.util.Arrays.copyOfRange(bytes, offset, offset + len);
        }
    }

    private record DirEntry(String name, int type, int startSector, long size) {
    }
}
