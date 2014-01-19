import java.util.Arrays;

public class Convert {
  private static final int[][] exponents = new int[][]{
      {11529, 2150460, 6846976},
      {  360, 2879701, 8963968},
      {   11, 2589990, 6842624},
      {    0, 3518437, 2088832},
      {    0,  109951, 1627776},
      {    0,    3435, 9738368},
      {    0,     107, 3741824},
      {    0,       3, 3554432},
      {    0,       0, 1048576},
      {    0,       0,   32768},
      {    0,       0,    1024},
      {    0,       0,      32},
      {    0,       0,       1}};

  // To convert a 64bit unsigned integer given as a base10 string into a base32 int[]
  // we first convert the string into a base10,000,000 number (i.e. 6, 7, 7 digits)
  // and then calculate the final base32 by continuously subtracting 32^n for the
  // biggest n that results in a remainder >=0.
  public static void main(String[] args) {
    String account = args[0];
    int[] base10mio = new int[3];
    base10mio[2] = Integer.parseInt(account.substring(
        Math.max(0, account.length() - 7)));
    if (account.length() > 7) {
      base10mio[1] = Integer.parseInt(account.substring(
          Math.max(0, account.length() - 14), account.length() - 7));
      if (account.length() > 14) {
        base10mio[0] = Integer.parseInt(account.substring(0, account.length() - 14));
      }
    }
    int[] base32 = new int[13];
    for (int i = 0; i < 13; i++) {
      int[] result = new int[3];
      while (subtract(base10mio, exponents[i], result)) {
        base32[i]++;
        if (i == 0 && base32[i] >= 16) {
          throw new IllegalArgumentException(account + " is bigger than 2^64-1.");
        }
        base10mio = result.clone();
      }
    }
    System.out.println(Arrays.toString(base32));
  }

  // calculates a-b=c in base 10mio and returns true iff the result is >=0
  // otherwise it returns false and c is undefined
  private static boolean subtract(int[] a, int[] b, int[] c) {
    int overflow = 0;
    for (int i = a.length - 1; i >= 0; i--) {
      c[i] = a[i] - overflow - b[i];
      if (c[i] < 0) {
        overflow = 1;
        c[i] += 10000000;
      } else {
        overflow = 0;
      }
    }
    if (overflow > 0) {
      return false;
    }
    return true;
  }
}
