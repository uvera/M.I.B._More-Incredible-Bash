package de.audi.atip.sysapp;

import de.audi.app.car.MenuCtrl;
import de.audi.atip.sysapp.carcoding.CarFuncAdap;
import java.util.Hashtable;

/**
 * M.I.B. Sport HMI classpath override: forces menu display for
 * {@link CarFuncAdap#CAR_FUNCTION_SPORT_HMI}. Stock implementation leaves this id unmapped in
 * {@code getByteCoding}, so {@code SDISBase#updateVisibility} returns hidden (0).
 *
 * {@code AbstractCarStateHandler}: {@code isClamp15Sensitive} is
 * {@code isMenuDisplayActivated && !isMenuDisClamp15OffActivated}. For Sport HMI we return
 * {@code true} for {@code isMenuDisClamp15OffActivated} so clamp-15 listeners are not registered
 * and the entry is not tied off when KL15 drops. {@code isStandstillSensitive} is
 * {@code isMenuDisplayActivated && isMenuDisStandstillActivated}; we force {@code false} for
 * Sport HMI so standstill-based registration stays off (same as unmapped {@code getByteCoding} 0).
 *
 * Uses {@link Hashtable} instead of {@code de.audi.atip.utils.generics.GMap}: some jxe2jar
 * outputs omit GMap.class; the map is private and only used like {@code java.util.Map}.
 *
 * See mod/java/sport_hmi/TARGETS.txt.
 */
public class CarFuncAdapImpl implements CarFuncAdap {

    private Hashtable carFuncToMenuOpFlagIdx = new Hashtable();

    public CarFuncAdapImpl() {
        this.carFuncToMenuOpFlagIdx.put(new Short((short) 21), new Integer(21));
        this.carFuncToMenuOpFlagIdx.put(new Short((short) 40), new Integer(40));
        this.carFuncToMenuOpFlagIdx.put(new Short((short) 11), new Integer(11));
        this.carFuncToMenuOpFlagIdx.put(new Short((short) 13), new Integer(13));
        this.carFuncToMenuOpFlagIdx.put(new Short((short) 10), new Integer(10));
        this.carFuncToMenuOpFlagIdx.put(new Short((short) 8), new Integer(8));
        this.carFuncToMenuOpFlagIdx.put(new Short((short) 32), new Integer(32));
        this.carFuncToMenuOpFlagIdx.put(new Short((short) 18), new Integer(18));
        this.carFuncToMenuOpFlagIdx.put(new Short((short) 19), new Integer(19));
    }

    public boolean isMenuDisplayActivated(short s) {
        if (s == CarFuncAdap.CAR_FUNCTION_SPORT_HMI) {
            return true;
        }
        return (this.getByteCoding(s) & 1) == 1;
    }

    public boolean isMenuDisClamp15OffActivated(short s) {
        if (s == CarFuncAdap.CAR_FUNCTION_SPORT_HMI) {
            return true;
        }
        return (this.getByteCoding(s) & 2) == 2;
    }

    public boolean isMenuDisOverThresholdHighActivated(short s) {
        return (this.getByteCoding(s) & 4) == 4;
    }

    public boolean isMenuDisStandstillActivated(short s) {
        if (s == CarFuncAdap.CAR_FUNCTION_SPORT_HMI) {
            return false;
        }
        return (this.getByteCoding(s) & 8) == 8;
    }

    public boolean isMenuDisAfterDisclaimerActivated(short s) {
        return false;
    }

    public byte getByteCoding(short s) {
        if (s == CarFuncAdap.CAR_FUNCTION_SPORT_HMI) {
            // Bit 0 = menu display on. OR in CMOF nibble so speed/clamp/standstill bits from
            // MenuCtrl are preserved; plain (byte)1 wiped those and could confuse other checks.
            return (byte) (MenuCtrl.getFlag(52) | 1);
        }
        if (this.carFuncToMenuOpFlagIdx.containsKey(new Short(s))) {
            int n = ((Integer) this.carFuncToMenuOpFlagIdx.get(new Short(s))).intValue();
            return MenuCtrl.getFlag(n);
        }
        return 0;
    }
}
