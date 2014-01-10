package com.github.ricotilgner.nxtreedsolomon;

import java.math.BigInteger;
import java.util.Arrays;

import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
import com.google.zxing.common.reedsolomon.ReedSolomonEncoder;
import com.google.zxing.common.reedsolomon.ReedSolomonException;

public class NxtReedSolomon {
  private static final String charSet =
      "#$0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  private static final BigInteger POW_2_64 = BigInteger.valueOf(2).pow(64);

  public static void main(String[] args) {
    BigInteger account = new BigInteger(args[0]);
    System.out.println(account.toString());
    String encoded = encode(account);
    System.out.println("encoded: " + encoded);
    try {
      System.out.println(decode(args[1]));
    } catch (IllegalArgumentException e) {
      System.out.println("Not valid!");
    } catch (CorrectionException e) {
      System.out.println("Did you mean: " + e.getCorrected());
    }
  }

  public static String encode(BigInteger account)
      throws IllegalArgumentException {
    if (account.compareTo(POW_2_64) >= 0) {
      throw new IllegalArgumentException("Account number is bigger than 2^64.");
    }
    if (account.compareTo(BigInteger.ZERO) < 0) {
      throw new IllegalArgumentException("Account number is negative.");
    }
    int[] accountBase64 = new int[15];
    for (int i = 0; i < 11; i++) {
      accountBase64[i] =
          account.shiftRight(60 - 6 * i).and(BigInteger.valueOf(63)).intValue();
    }
    new ReedSolomonEncoder(GenericGF.MAXICODE_FIELD_64).encode(accountBase64, 4);
    return base64ToString(accountBase64);
  }

  private static String base64ToString(int[] base64) {
    StringBuffer output = new StringBuffer();
    for (int i = 0; i < base64.length; i++) {
      output.append(charSet.charAt(base64[i]));
      if (i % 5 == 4) {
        output.append(" ");
      }
    }
    return output.toString();
  }

  public static BigInteger decode(String account)
      throws IllegalArgumentException, CorrectionException {
    account = account.replace(" ", "");
    if (account.length() != 15) {
      throw new IllegalArgumentException("Account name has the wrong length.");
    }
    if (!account.matches("^[#$0-9A-Za-z]+$")) {
      throw new IllegalArgumentException("Account name contains illegal characters.");
    }
    int[] accountBase64 = new int[15];
    for (int i = 0; i < 15; i++) {
      accountBase64[i] = charSet.indexOf(account.charAt(i));
    }
    int[] inputAccountBase64 = Arrays.copyOf(accountBase64, accountBase64.length);
    try {
      new ReedSolomonDecoder(GenericGF.MAXICODE_FIELD_64).decode(accountBase64, 4);
    } catch (ReedSolomonException e) {
      throw new IllegalArgumentException("Account does not exist.");
    }
    if (!Arrays.equals(inputAccountBase64, accountBase64)) {
      throw new CorrectionException(base64ToString(accountBase64));
    }
    BigInteger result = BigInteger.ZERO;
    for (int i = 0; i < 11; i++) {
      result = result.add(BigInteger.valueOf(accountBase64[i])
          .and(BigInteger.valueOf(63)).shiftLeft(60 - 6 * i));
    }
    // If the input contained more errors than RS could handle, we can still make sure
    // the 2 Bit we had to append while encoding are 0.
    if (result.compareTo(POW_2_64) >= 0) {
      throw new IllegalArgumentException("Account does not exist.");
    }
    return result;
  }
}
