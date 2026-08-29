package org.pangya.protocol.crypto;

import java.util.Arrays;

/**
 * Faithful port of {@code PangyaAPI.Network.Cryptor.MiniLzo} (LZO1X-1).
 */
public final class MiniLzo {

    private MiniLzo() {}

    public static byte[] compress(byte[] input) {
        byte[] out = new byte[input.length + input.length / 16 + 64 + 3];
        int outLen = lzo1x1Compress(input, input.length, out, new short[32768]);
        return Arrays.copyOf(out, outLen);
    }

    public static byte[] decompress(byte[] input) {
        try {
            return lzo1xDecompress(input);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Input buffer ends too early", e);
        }
    }

    private static int readU32(byte[] arr, int i) {
        return (arr[i] & 0xff)
                | ((arr[i + 1] & 0xff) << 8)
                | ((arr[i + 2] & 0xff) << 16)
                | ((arr[i + 3] & 0xff) << 24);
    }

    private static int readU16(byte[] arr, int i) {
        return (arr[i] & 0xff) | ((arr[i + 1] & 0xff) << 8);
    }

    private static int nearestPowerOfTwo(int n) {
        n--;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return n + 1;
    }

    private static final class Buf {
        byte[] a = new byte[0];

        void ensure(int pos, int need) {
            int needLen = pos + need;
            if (needLen > a.length) {
                a = Arrays.copyOf(a, nearestPowerOfTwo(Math.max(needLen, 1)));
            }
        }
    }

    private static byte[] lzo1xDecompress(byte[] in) {
        Buf out = new Buf();
        int op = 0;
        int ip = 0;
        boolean firstLiteral = false;
        boolean matchDone = false;
        int t = 0;
        int mPos;

        if ((in[ip] & 0xff) > 17) {
            t = (in[ip++] & 0xff) - 17;
            out.ensure(op, t);
            while (t > 0) {
                out.a[op++] = in[ip++];
                t--;
            }
            if (t >= 4) {
                firstLiteral = true;
            }
        }

        outer:
        while (true) {
            if (!firstLiteral) {
                t = in[ip++] & 0xff;
                if (t < 16) {
                    if (t == 0) {
                        while (in[ip] == 0) {
                            t += 255;
                            ip++;
                        }
                        t += 15 + (in[ip++] & 0xff);
                    }
                    t += 3;
                    out.ensure(op, t);
                    while (t > 0) {
                        out.a[op++] = in[ip++];
                        t--;
                    }
                    t = in[ip++] & 0xff;
                    if (t >= 16) {
                        matchDone = false;
                    } else {
                        mPos = op - (1 + 0x0800);
                        mPos -= t >>> 2;
                        mPos -= (in[ip++] & 0xff) << 2;
                        out.ensure(op, 3);
                        out.a[op++] = out.a[mPos++];
                        out.a[op++] = out.a[mPos++];
                        out.a[op++] = out.a[mPos];
                        matchDone = true;
                    }
                } else {
                    matchDone = false;
                }
            } else {
                firstLiteral = false;
                t = in[ip++] & 0xff;
                if (t >= 16) {
                    matchDone = false;
                } else {
                    mPos = op - (1 + 0x0800);
                    mPos -= t >>> 2;
                    mPos -= (in[ip++] & 0xff) << 2;
                    out.ensure(op, 3);
                    out.a[op++] = out.a[mPos++];
                    out.a[op++] = out.a[mPos++];
                    out.a[op++] = out.a[mPos];
                    matchDone = true;
                }
            }

            do {
                if (!matchDone) {
                    if (t >= 64) {
                        mPos = op - 1;
                        mPos -= (t >>> 2) & 7;
                        mPos -= (in[ip++] & 0xff) << 3;
                        t = (t >>> 5) - 1;
                        t += 2;
                        out.ensure(op, t);
                        while (t > 0) {
                            out.a[op++] = out.a[mPos++];
                            t--;
                        }
                    } else if (t >= 32) {
                        t &= 31;
                        if (t == 0) {
                            while (in[ip] == 0) {
                                t += 255;
                                ip++;
                            }
                            t += 31 + (in[ip++] & 0xff);
                        }
                        mPos = op - 1;
                        mPos -= readU16(in, ip) >>> 2;
                        ip += 2;
                        t += 2;
                        out.ensure(op, t);
                        while (t > 0) {
                            out.a[op++] = out.a[mPos++];
                            t--;
                        }
                    } else if (t >= 16) {
                        mPos = op;
                        mPos -= (t & 8) << 11;
                        t &= 7;
                        if (t == 0) {
                            while (in[ip] == 0) {
                                t += 255;
                                ip++;
                            }
                            t += 7 + (in[ip++] & 0xff);
                        }
                        mPos -= readU16(in, ip) >>> 2;
                        ip += 2;
                        if (mPos == op) {
                            break outer;
                        }
                        mPos -= 0x4000;
                        t += 2;
                        out.ensure(op, t);
                        while (t > 0) {
                            out.a[op++] = out.a[mPos++];
                            t--;
                        }
                    } else {
                        mPos = op - 1;
                        mPos -= t >>> 2;
                        mPos -= (in[ip++] & 0xff) << 2;
                        out.ensure(op, 2);
                        out.a[op++] = out.a[mPos++];
                        out.a[op++] = out.a[mPos];
                    }
                }
                matchDone = false;
                t = in[ip - 2] & 3;
                if (t == 0) {
                    break;
                }
                out.ensure(op, t);
                while (t > 0) {
                    out.a[op++] = in[ip++];
                    t--;
                }
                t = in[ip++] & 0xff;
            } while (true);
        }
        return Arrays.copyOf(out.a, op);
    }

    private static int lzo1x1Compress(byte[] in, int inLen, byte[] out, short[] dict) {
        int ip = 0;
        int op = 0;
        int l = inLen;
        int t = 0;
        while (l > 20) {
            int ll = Math.min(l, 49152);
            int llEnd = ip + ll;
            if (llEnd + ((t + ll) >>> 5) <= llEnd || llEnd + ((t + ll) >>> 5) <= ip + ll) {
                break;
            }
            Arrays.fill(dict, (short) 0);
            int[] core = lzo1x1CompressCore(in, ip, ll, out, op, t, dict);
            t = core[0];
            int outLen = core[1];
            ip += ll;
            op += outLen;
            l -= ll;
        }
        t += l;
        if (t > 0) {
            int ii = inLen - t;
            if (op == 0 && t <= 238) {
                out[op++] = (byte) (17 + t);
            } else if (t <= 3) {
                out[op - 2] |= (byte) t;
            } else if (t <= 18) {
                out[op++] = (byte) (t - 3);
            } else {
                int tt = t - 18;
                out[op++] = 0;
                while (tt > 255) {
                    tt -= 255;
                    out[op++] = 0;
                }
                out[op++] = (byte) tt;
            }
            do {
                out[op++] = in[ii++];
            } while (--t > 0);
        }
        out[op++] = 16 | 1;
        out[op++] = 0;
        out[op++] = 0;
        return op;
    }

    private static int[] lzo1x1CompressCore(byte[] in, int inIndex, int inLen, byte[] out, int outIndex, int ti, short[] dict) {
        int inEnd = inIndex + inLen;
        int ipEnd = inIndex + inLen - 20;
        int op = outIndex;
        int ip = inIndex;
        int ii = ip;
        ip += ti < 4 ? 4 - ti : 0;

        outer:
        for (;;) {
            ip += 1 + ((ip - ii) >>> 5);
            inner:
            for (;;) {
                if (ip >= ipEnd) {
                    break outer;
                }
                int dv = readU32(in, ip);
                int dIndex = ((0x1824429d * dv) >>> 18) & 0x3fff;
                int mPos = inIndex + (dict[dIndex] & 0xffff);
                dict[dIndex] = (short) (ip - inIndex);
                if (dv != readU32(in, mPos)) {
                    break inner;
                }
                ii -= ti;
                ti = 0;
                int t = ip - ii;
                if (t != 0) {
                    if (t <= 3) {
                        out[op - 2] |= (byte) t;
                        System.arraycopy(in, ii, out, op, t);
                        op += t;
                    } else if (t <= 16) {
                        out[op++] = (byte) (t - 3);
                        System.arraycopy(in, ii, out, op, t);
                        op += t;
                    } else {
                        if (t <= 18) {
                            out[op++] = (byte) (t - 3);
                        } else {
                            int tt = t - 18;
                            out[op++] = 0;
                            while (tt > 255) {
                                tt -= 255;
                                out[op++] = 0;
                            }
                            out[op++] = (byte) tt;
                        }
                        System.arraycopy(in, ii, out, op, t);
                        op += t;
                    }
                }
                int mLen = 4;
                int v = readU32(in, ip + mLen) ^ readU32(in, mPos + mLen);
                while (v == 0) {
                    mLen += 4;
                    v = readU32(in, ip + mLen) ^ readU32(in, mPos + mLen);
                    if (ip + mLen >= ipEnd) {
                        break;
                    }
                }
                if (ip + mLen < ipEnd) {
                    mLen += Integer.numberOfTrailingZeros(v) / 8;
                }
                int mOff = ip - mPos;
                ip += mLen;
                ii = ip;
                if (mLen <= 8 && mOff <= 0x0800) {
                    mOff -= 1;
                    out[op++] = (byte) (((mLen - 1) << 5) | ((mOff & 7) << 2));
                    out[op++] = (byte) (mOff >>> 3);
                } else if (mOff <= 0x4000) {
                    mOff -= 1;
                    if (mLen <= 33) {
                        out[op++] = (byte) (32 | (mLen - 2));
                    } else {
                        mLen -= 33;
                        out[op++] = 32;
                        while (mLen > 255) {
                            mLen -= 255;
                            out[op++] = 0;
                        }
                        out[op++] = (byte) mLen;
                    }
                    out[op++] = (byte) (mOff << 2);
                    out[op++] = (byte) (mOff >>> 6);
                } else {
                    mOff -= 0x4000;
                    if (mLen <= 9) {
                        out[op++] = (byte) (16 | ((mOff >>> 11) & 8) | (mLen - 2));
                    } else {
                        mLen -= 9;
                        out[op++] = (byte) (16 | ((mOff >>> 11) & 8));
                        while (mLen > 255) {
                            mLen -= 255;
                            out[op++] = 0;
                        }
                        out[op++] = (byte) mLen;
                    }
                    out[op++] = (byte) (mOff << 2);
                    out[op++] = (byte) (mOff >>> 6);
                }
            }
        }
        return new int[] {inEnd - (ii - ti), op - outIndex};
    }
}
