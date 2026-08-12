package me.gamerduck.alwaysauth.api.updates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FlexVerComparator {

    public static int compare(String a, String b) {
        List<VersionComponent> ad = decompose(a);
        List<VersionComponent> bd = decompose(b);
        for (int i = 0; i < Math.max(ad.size(), bd.size()); i++) {
            int c = get(ad, i).compareTo(get(bd, i));
            if (c != 0) return c;
        }
        return 0;
    }

    private static final VersionComponent NULL = new VersionComponent(new int[0]) {
        @Override
        public int compareTo(VersionComponent other) { return other == NULL ? 0 : -other.compareTo(this); }
    };

    static class VersionComponent {
        private final int[] codepoints;

        public VersionComponent(int[] codepoints) {
            this.codepoints = codepoints;
        }

        public int[] codepoints() {
            return codepoints;
        }

        public int compareTo(VersionComponent that) {
            if (that == NULL) return 1;
            int[] a = this.codepoints();
            int[] b = that.codepoints();

            for (int i = 0; i < Math.min(a.length, b.length); i++) {
                int c1 = a[i];
                int c2 = b[i];
                if (c1 != c2) return c1 - c2;
            }

            return a.length - b.length;
        }

        @Override
        public String toString() {
            return new String(codepoints, 0, codepoints.length);
        }

    }

    static class SemVerPrereleaseVersionComponent extends VersionComponent {
        public SemVerPrereleaseVersionComponent(int[] codepoints) { super(codepoints); }

        @Override
        public int compareTo(VersionComponent that) {
            if (that == NULL) return -1;
            return super.compareTo(that);
        }

    }

    static class NumericVersionComponent extends VersionComponent {
        public NumericVersionComponent(int[] codepoints) { super(codepoints); }

        @Override
        public int compareTo(VersionComponent that) {
            if (that == NULL) return 1;
            if (that instanceof NumericVersionComponent) {
                int[] a = removeLeadingZeroes(this.codepoints());
                int[] b = removeLeadingZeroes(that.codepoints());
                if (a.length != b.length) return a.length-b.length;
                for (int i = 0; i < a.length; i++) {
                    int ad = a[i];
                    int bd = b[i];
                    if (ad != bd) return ad-bd;
                }
                return 0;
            }
            return super.compareTo(that);
        }

        private int[] removeLeadingZeroes(int[] a) {
            if (a.length == 1) return a;
            int i = 0;
            int stopIdx = a.length - 1;
            while (i < stopIdx && a[i] == '0') {
                i++;
            }
            return Arrays.copyOfRange(a, i, a.length);
        }

    }

    static List<VersionComponent> decompose(String str) {
        if (str.isEmpty()) return Collections.emptyList();
        boolean lastWasNumber = isAsciiDigit(str.codePointAt(0));
        int totalCodepoints = str.codePointCount(0, str.length());
        int[] accum = new int[totalCodepoints];
        List<VersionComponent> out = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < str.length(); i++) {
            int cp = str.codePointAt(i);
            if (Character.charCount(cp) == 2) i++;
            if (cp == '+') break;
            boolean number = isAsciiDigit(cp);
            if (number != lastWasNumber || (cp == '-' && j > 0 && accum[0] != '-')) {
                out.add(createComponent(lastWasNumber, accum, j));
                j = 0;
                lastWasNumber = number;
            }
            accum[j] = cp;
            j++;
        }
        out.add(createComponent(lastWasNumber, accum, j));
        return out;
    }

    private static boolean isAsciiDigit(int cp) {
        return cp >= '0' && cp <= '9';
    }

    private static VersionComponent createComponent(boolean number, int[] s, int j) {
        s = Arrays.copyOfRange(s, 0, j);
        if (number) {
            return new NumericVersionComponent(s);
        } else if (s.length > 1 && s[0] == '-') {
            return new SemVerPrereleaseVersionComponent(s);
        } else {
            return new VersionComponent(s);
        }
    }

    private static VersionComponent get(List<VersionComponent> li, int i) {
        return i >= li.size() ? NULL : li.get(i);
    }

}
