
package com.sw.sun.common.string;

public class RC4Cryption {
    static private int keylength = 8; // keylength for WEP; deprecated

    private byte[] S;

    private int the_i;

    private int the_j;

    private int next_j = -666; // not really needed after all

    public RC4Cryption() {
        S = new byte[256];
        the_i = the_j = 0;
    }

    public RC4Cryption(byte[] S) {
        this.S = S;
        the_i = the_j = 0;
    }

    /**
     * run the key scheduler for n rounds, using key[0]...key[n-1]
     */
    private void ksa(int n, byte[] key, boolean printstats) {
        int keylength = key.length; // NOT keylength above!!
        int i = 0;
        for (i = 0; i < 256; i++)
            S[i] = (byte) i;
        the_j = 0;
        for (the_i = 0; the_i < n; the_i++) {
            the_j = (the_j + posify(S[the_i]) + posify(key[the_i % keylength])) % 256;
            sswap(S, the_i, the_j);
        }
        if (n != 256) {
            next_j = (the_j + posify(S[n]) + posify(key[n % keylength])) % 256;
        }
        if (printstats) {
            System.out.print("S_" + (n - 1) + ":");
            for (int k = 0; k <= n; k++)
                System.out.print(" " + posify(S[k]));
            System.out.print("   j_" + (n - 1) + "=" + the_j);
            System.out.print("   j_" + n + "=" + next_j);
            System.out.print("   S_" + (n - 1) + "[j_" + (n - 1) + "]=" + posify(S[the_j]));
            System.out.print("   S_" + (n - 1) + "[j_" + (n) + "]=" + posify(S[next_j]));
            if (S[1] != 0)
                System.out.print("   S[1]!=0");
            System.out.println();
        }
    }

    private void ksa(byte[] key) {
        ksa(256, key, false);
    }

    private void init() {
        the_i = the_j = 0;
    }

    byte nextVal() {
        the_i = (the_i + 1) % 256;
        the_j = (the_j + posify(S[the_i])) % 256;
        sswap(S, the_i, the_j);
        byte value = S[(posify(S[the_i]) + posify(S[the_j])) % 256];
        return value;
    }

    // returns i for which x = S[i]
    byte inverse(byte x) {
        int i = 0;
        while (i < 256) {
            if (x == S[i])
                return (byte) i;
            i++;
        }
        return (byte) 0; // never get here
    }

    int the_i() {
        return this.the_i;
    }

    int the_j() {
        return this.the_j;
    }

    int next_j() {
        return this.next_j;
    }

    int S(int n) {
        return posify(S[(byte) n]);
    }

    private static void sswap(byte[] S, int i, int j) {
        byte temp = S[i];
        S[i] = S[j];
        S[j] = temp;
    }

    // returns value of b as an unsigned int
    public static int posify(byte b) {
        if (b >= 0)
            return b;
        else
            return 256 + b;
    }

    /**
     * buildkey is for WEP keys only
     */
    public static byte[] buildkey(byte[] IV, byte[] shortkey) {
        byte[] key = new byte[keylength];
        int ivlen = IV.length;
        int i = 0;
        for (i = 0; i < ivlen; i++)
            key[i] = IV[i];
        for (i = ivlen; i < keylength; i++)
            key[i] = shortkey[i - ivlen];
        return key;
    }

    static public String byte2string(byte b) {
        int high = (b >> 4) & 0x0F;
        int low = (b & 0x0F);
        String convert = "0123456789abcdef";
        // convert = "0123456789ABCDEF"; // uncomment if you want uppercase
        String result = "";
        result += convert.charAt(high);
        result += convert.charAt(low);
        return result;
    }

    static public String byte2string(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += byte2string(b[i]);
        }
        return result;
    }

    // given a char '0' ...'f' or 'F', returns 0..15
    static private int hexval(char c) {
        if ('0' <= c && c <= '9')
            return (c - '0');
        if ('a' <= c && c <= 'f')
            return (c - 'a' + 10);
        if ('A' <= c && c <= 'F')
            return (c - 'A' + 10);
        return 0;
    }

    static public byte[] string2byte(String s) {
        int length = s.length();
        length = (length / 2);
        byte[] buf = new byte[length];
        for (int i = 0; i < length; i++) {
            int nyb1 = hexval(s.charAt(2 * i));
            int nyb2 = hexval(s.charAt(2 * i + 1));
            buf[i] = (byte) ((nyb1 * 16) + nyb2);
        }
        return buf;
    }

    /**
     * encrypt is for testing; key can be any length
     */
    public static byte[] encrypt(byte[] key, byte[] content) {
        byte[] outbuf = new byte[content.length];
        RC4Cryption r = new RC4Cryption();
        r.ksa(key);
        r.init();
        for (int i = 0; i < content.length; i++) {
            outbuf[i] = (byte) (content[i] ^ r.nextVal());
        }
        return outbuf;
    }

    public static String encrypt(byte[] key, String content) {
        byte[] contentBytes = content.getBytes();
        byte[] encrypted = encrypt(key, contentBytes);
        return String.valueOf(Base64Coder.encode(encrypted));
    }

    public static byte[] decrypt(byte[] key, String content) {
        byte[] contentBytes = Base64Coder.decode(content);
        return encrypt(key, contentBytes);
    }

    public static byte[] generateKeyForRC4(String secretKey, String id) {
        byte[] keyBytes = Base64Coder.decode(secretKey);
        byte[] idbytes = id.getBytes();
        byte[] result = new byte[keyBytes.length + 1 + idbytes.length];
        for (int i = 0; i < keyBytes.length; ++i) {
            result[i] = keyBytes[i];
        }
        result[keyBytes.length] = '_';
        for (int i = 0; i < idbytes.length; ++i) {
            result[keyBytes.length + 1 + i] = idbytes[i];
        }
        return result;
    }
}
