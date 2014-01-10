package com.github.ricotilgner.nxtreedsolomon;

public class CorrectionException extends Exception {
  private final String corrected;
  
  public CorrectionException(String corrected) {
    super();
    this.corrected = corrected;
  }

  public String getCorrected() {
    return corrected;
  }
}
