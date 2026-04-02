package de.audi.app.car.sdis.base;

import de.audi.atip.log.LogChannel;
import de.audi.atip.sysapp.carcoding.CarFuncAdap;
import org.dsi.ifc.global.CarViewOption;

/**
 * M.I.B. Sport HMI: stock {@code getVisibilityState(CarViewOption, short)} uses
 * {@code getByteCoding(s)} and then {@code isMenuDisplayActivated((short)byte)}. For
 * {@code CAR_FUNCTION_SPORT_HMI} (52), {@code CarFuncAdapImpl} returns {@code getByteCoding(52)==1},
 * so stock code calls {@code isMenuDisplayActivated(1)} (wrong id) and returns hidden. Bypass that
 * for 52 and mirror the single-argument mapping.
 *
 * See mod/java/sport_hmi/TARGETS.txt.
 */
public class ViewOptionMapper {

    private final LogChannel logger;
    private final CarFuncAdap codingAdapter;

    public ViewOptionMapper(LogChannel logChannel, CarFuncAdap carFuncAdap) {
        this.logger = logChannel;
        this.codingAdapter = carFuncAdap;
    }

    public int getVisibilityState(CarViewOption carViewOption, short s) {
        if (s == (short) 52) {
            return this.getVisibilityState(carViewOption);
        }
        byte b = this.codingAdapter.getByteCoding(s);
        short s2 = (short) b;
        if (!this.codingAdapter.isMenuDisplayActivated(s2)) {
            return 0;
        }
        if (carViewOption == null) {
            this.logger.log(1000, 293);
            return 1;
        }
        int state = carViewOption.getState();
        int out;
        if (state == 2) {
            out = 2;
        } else if (state == 1) {
            out = this.getDisabledState(carViewOption.getReason());
        } else {
            out = 1;
        }
        return out;
    }

    public int getVisibilityState(CarViewOption carViewOption) {
        if (carViewOption == null) {
            this.logger.log(1000, 294);
            return 1;
        }
        int state = carViewOption.getState();
        int out;
        if (state == 2) {
            out = 2;
        } else if (state == 1) {
            out = this.getDisabledState(carViewOption.getReason());
        } else {
            out = 1;
        }
        return out;
    }

    public int getVisibilityState(CarViewOption[] carViewOptionArray) {
        int n = 2;
        int i = 0;
        while (i < carViewOptionArray.length) {
            int v = this.getVisibilityState(carViewOptionArray[i]);
            if (v == 1) {
                n = v;
            } else if (v == 2) {
                /* stock: no merge */
            } else if (v == 6) {
                /* stock: no merge */
            } else if (n != 1 && (v == 3 || v == 4 || v == 5 || v == 7 || v == 8 || v == 9)) {
                n = this.getMaxState(n, v);
            }
            i = i + 1;
        }
        return n;
    }

    private int getDisabledState(int n) {
        int out;
        if (n == 2) {
            out = 4;
        } else if (n == 3) {
            out = 5;
        } else if (n == 4) {
            out = 7;
        } else if (n == 5) {
            out = 4;
        } else if (n == 6) {
            out = 8;
        } else {
            out = 3;
        }
        return out;
    }

    private int getMaxState(int n, int n2) {
        if (n == 1 || n2 == 1) {
            return 1;
        }
        if (n == 9 || n2 == 9) {
            return 9;
        }
        if (n == 3 || n2 == 3) {
            return 3;
        }
        if (n == 4 || n2 == 4) {
            return 4;
        }
        if (n == 7 || n2 == 7) {
            return 7;
        }
        if (n == 5 || n2 == 5) {
            return 5;
        }
        if (n == 8 || n2 == 8) {
            return 8;
        }
        return 2;
    }
}
