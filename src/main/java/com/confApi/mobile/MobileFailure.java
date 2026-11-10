package com.confApi.mobile;

final class MobileFailure extends RuntimeException {
    final int status;
    final String code;
    MobileFailure(int status,String code) { super(code); this.status=status; this.code=code; }
}
