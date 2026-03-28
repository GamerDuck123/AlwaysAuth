/*
 * To the extent possible under law, the author has dedicated all copyright
 * and related and neighboring rights to this software to the public domain
 * worldwide. This software is distributed without any warranty.
 *
 * See <http://creativecommons.org/publicdomain/zero/1.0/>
 */

package me.gamerduck.alwaysauth.api.updates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Implements FlexVer, a SemVer-compatible intuitive comparator for free-form versioning strings.
 * <p>
 * FlexVer is designed to sort versions like people do, rather than attempting to force
 * conformance to a rigid and limited standard. It imposes no restrictions on version format,
 * making it ideal for real-world version strings commonly seen in Minecraft mods and plugins.
 * </p>
 * <p>
 * <b>Key Features:</b>
 * <ul>
 *     <li>Handles numeric components intelligently (e.g., 1.10 > 1.9, not lexicographic)</li>
 *     <li>Supports SemVer prerelease tags (versions starting with '-' are sorted lower)</li>
 *     <li>Breaks versions at numeric/non-numeric boundaries for natural comparison</li>
 *     <li>Handles versions of differing lengths gracefully</li>
 *     <li>Ignores appendices after '+' character (SemVer build metadata)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Example Comparisons:</b>
 * <ul>
 *     <li>{@code 1.2.3 < 1.2.10} (numeric comparison, not lexicographic)</li>
 *     <li>{@code 1.0.0-alpha < 1.0.0} (prerelease versions sort lower)</li>
 *     <li>{@code 1.0+build123 == 1.0+build456} (build metadata ignored)</li>
 *     <li>{@code 1.0 < 1.0.1} (shorter versions sort lower)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Warning:</b> Comparing two versions with completely different formats may produce
 * unexpected results (garbage in, garbage out). This comparator works best when versions
 * follow similar structural patterns.
 * </p>
 * <p>
 * <b>Attribution:</b>
 * Original implementation from <a href="https://github.com/Patbox/simple-update-checker/blob/master/src/main/java/eu/pb4/simpleupdatechecker/FlexVerComparator.java">Patbox</a>,
 * edited by GamerDuck123.
 * </p>
 *
 * @see <a href="https://semver.org/">Semantic Versioning Specification</a>
 */
public class FlexVerComparator {

    /**
     * Compares two version strings according to FlexVer rules.
     * <p>
     * Parses both version strings into components, then compares them component-by-component.
     * Numeric components are compared numerically, non-numeric components lexicographically,
     * and prerelease tags (starting with '-') are sorted lower than regular versions.
     * </p>
     * <p>
     * <b>Examples:</b>
     * <pre>
     * compare("1.2.3", "1.2.10")    returns negative (1.2.3 &lt; 1.2.10)
     * compare("2.0.0", "1.9.9")     returns positive (2.0.0 &gt; 1.9.9)
     * compare("1.0-alpha", "1.0")   returns negative (prerelease &lt; release)
     * compare("1.0", "1.0")         returns 0 (equal)
     * compare("1.0+build", "1.0")   returns 0 (build metadata ignored)
     * </pre>
     * </p>
     *
     * @param a the first version string to compare
     * @param b the second version string to compare
     * @return {@code 0} if the versions are equal, a negative number if {@code a < b},
     *         or a positive number if {@code a > b}
     */
    public static int compare(String a, String b) {
        List<VersionComponent> ad = decompose(a);
        List<VersionComponent> bd = decompose(b);
        for (int i = 0; i < Math.max(ad.size(), bd.size()); i++) {
            int c = get(ad, i).compareTo(get(bd, i));
            if (c != 0) return c;
        }
        return 0;
    }


    /**
     * Sentinel component representing a missing version component.
     * <p>
     * Used when comparing versions of different lengths. For example, when comparing
     * "1.2.3" with "1.2", the third component of the second version is NULL.
     * NULL components sort lower than any actual component.
     * </p>
     */
    private static final VersionComponent NULL = new VersionComponent(new int[0]) {
        @Override
        public int compareTo(VersionComponent other) { return other == NULL ? 0 : -other.compareTo(this); }
    };

    /**
     * Base class representing a single component of a version string.
     * <p>
     * A component is a contiguous sequence of either numeric or non-numeric characters.
     * For example, "1.2.3-alpha" decomposes into components: ["1", ".", "2", ".", "3", "-alpha"].
     * </p>
     * <p>
     * Components are compared lexicographically by their Unicode codepoints, except for
     * specialized subclasses which override comparison behavior.
     * </p>
     */
    // @VisibleForTesting
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

    /**
     * Version component representing a SemVer prerelease identifier.
     * <p>
     * Prerelease components start with a hyphen '-' (e.g., "-alpha", "-beta", "-rc1").
     * According to SemVer, prerelease versions sort LOWER than release versions.
     * </p>
     * <p>
     * <b>Examples:</b>
     * <ul>
     *     <li>{@code 1.0.0-alpha < 1.0.0}</li>
     *     <li>{@code 1.0.0-beta < 1.0.0}</li>
     *     <li>{@code 1.0.0-rc1 < 1.0.0}</li>
     * </ul>
     * </p>
     *
     * @see <a href="https://semver.org/#spec-item-9">SemVer Prerelease Specification</a>
     */
    // @VisibleForTesting
    static class SemVerPrereleaseVersionComponent extends VersionComponent {
        public SemVerPrereleaseVersionComponent(int[] codepoints) { super(codepoints); }

        @Override
        public int compareTo(VersionComponent that) {
            if (that == NULL) return -1; // opposite order - prerelease sorts lower
            return super.compareTo(that);
        }

    }

    /**
     * Version component representing a numeric value.
     * <p>
     * Numeric components are compared numerically rather than lexicographically.
     * This ensures that "1.10" is correctly considered greater than "1.9".
     * </p>
     * <p>
     * Leading zeros are removed before comparison, so "007" equals "7".
     * However, a single zero is preserved (e.g., "0" remains "0", not empty).
     * </p>
     * <p>
     * <b>Examples:</b>
     * <ul>
     *     <li>{@code 9 < 10} (numeric: 9 &lt; 10, not lexicographic where "9" &gt; "10")</li>
     *     <li>{@code 007 == 7} (leading zeros removed)</li>
     *     <li>{@code 100 < 1000} (compared by length first, then digit-by-digit)</li>
     * </ul>
     * </p>
     */
    // @VisibleForTesting
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

    /**
     * Decomposes a version string into individual components.
     * <p>
     * Splits the version string at boundaries where character type changes from numeric to
     * non-numeric (or vice versa). Also handles special cases:
     * <ul>
     *     <li>Stops parsing at '+' character (SemVer build metadata separator)</li>
     *     <li>Treats '-' at the start of a component as a prerelease marker</li>
     *     <li>Handles Unicode supplementary characters correctly</li>
     * </ul>
     * </p>
     * <p>
     * <b>Examples:</b>
     * <ul>
     *     <li>{@code "1.2.3"} → {@code ["1", ".", "2", ".", "3"]}</li>
     *     <li>{@code "1.0-alpha"} → {@code ["1", ".", "0", "-alpha"]}</li>
     *     <li>{@code "1.0+build"} → {@code ["1", ".", "0"]} (build metadata ignored)</li>
     *     <li>{@code "v1.2"} → {@code ["v", "1", ".", "2"]}</li>
     * </ul>
     * </p>
     *
     * @param str the version string to decompose
     * @return a list of version components, or an empty list if the string is empty
     */
    // @VisibleForTesting
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
            if (cp == '+') break; // remove appendices
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