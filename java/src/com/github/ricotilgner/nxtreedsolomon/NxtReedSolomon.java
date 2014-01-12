package com.github.ricotilgner.nxtreedsolomon;

import java.math.BigInteger;
import java.util.Arrays;

import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
import com.google.zxing.common.reedsolomon.ReedSolomonEncoder;
import com.google.zxing.common.reedsolomon.ReedSolomonException;

public class NxtReedSolomon {
  private static final String charSet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
  private static final BigInteger POW_2_64 = BigInteger.valueOf(2).pow(64);
  private static final int DATA_LENGTH = 13;
  private static final int RS_LENGTH = 4;
  private static final int MESSAGE_LENGTH = DATA_LENGTH + RS_LENGTH;

  public static void main(String[] args) {
    BigInteger account = new BigInteger(args[0]);
    System.out.println(account.toString());
    String encoded = encode(account);
    System.out.println("encoded: " + encoded);
    try {
      System.out.println(decode(args[1]));
    } catch (IllegalArgumentException e) {
      System.out.println(e.getMessage());
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
    int[] accountBase32 = new int[MESSAGE_LENGTH];
    for (int i = 0; i < DATA_LENGTH; i++) {
      accountBase32[i] =
          account.shiftRight(60 - 5 * i).and(BigInteger.valueOf(31)).intValue();
    }
    new ReedSolomonEncoder(GenericGF.ITPP_32).encode(accountBase32, RS_LENGTH);
    return base32ToString(accountBase32);
  }

  private static String base32ToString(int[] base32) {
    StringBuffer output = new StringBuffer("NXT");
    for (int i = 0; i < base32.length; i++) {
      if (i % 5 == 2) {
        output.append("-");
      }
      output.append(charSet.charAt(base32[i]));
    }
    return output.toString();
  }

  public static BigInteger decode(String account)
      throws IllegalArgumentException, CorrectionException {
    account = account.trim().replace("-", "");
    if (!account.startsWith("NXT")) {
      throw new IllegalArgumentException("Account is not an NXT account.");
    }
    account = account.substring(3);
    if (account.length() != MESSAGE_LENGTH) {
      throw new IllegalArgumentException("Account name has the wrong length.");
    }
    if (!account.matches("^[2-9A-HJ-NP-Z]+$")) {
      throw new IllegalArgumentException("Account name contains illegal characters.");
    }
    int[] accountBase32 = new int[MESSAGE_LENGTH];
    for (int i = 0; i < MESSAGE_LENGTH; i++) {
      accountBase32[i] = charSet.indexOf(account.charAt(i));
    }
    int[] inputAccountBase32 = Arrays.copyOf(accountBase32, accountBase32.length);
    try {
      new ReedSolomonDecoder(GenericGF.ITPP_32).decode(accountBase32, RS_LENGTH);
    } catch (ReedSolomonException e) {
      throw new IllegalArgumentException("Account does not exist.");
    }
    if (!Arrays.equals(inputAccountBase32, accountBase32)) {
      throw new CorrectionException(base32ToString(accountBase32));
    }
    BigInteger result = BigInteger.ZERO;
    for (int i = 0; i < DATA_LENGTH; i++) {
      result = result.add(BigInteger.valueOf(accountBase32[i])
          .and(BigInteger.valueOf(31)).shiftLeft(60 - 5 * i));
    }
    // If the input contained more errors than RS could handle, we can still make sure
    // the 1 Bit we had to append while encoding are 0.
    if (result.compareTo(POW_2_64) >= 0) {
      throw new IllegalArgumentException("Account does not exist.");
    }
    return result;
  }
}
