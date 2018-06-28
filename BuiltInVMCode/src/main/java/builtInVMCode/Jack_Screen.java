/********************************************************************************
 * The contents of this file are subject to the GNU General Public License      *
 * (GPL) Version 2 or later (the "License"); you may not use this file except   *
 * in compliance with the License. You may obtain a copy of the License at      *
 * http://www.gnu.org/copyleft/gpl.html                                         *
 *                                                                              *
 * Software distributed under the License is distributed on an "AS IS" basis,   *
 * without warranty of any kind, either expressed or implied. See the License   *
 * for the specific language governing rights and limitations under the         *
 * License.                                                                     *
 *                                                                              *
 * This file was originally developed as part of the software suite that        *
 * supports the book "The Elements of Computing Systems" by Nisan and Schocken, *
 * MIT Press 2005. If you modify the contents of this file, please document and *
 * mark your changes clearly, for the benefit of others.                        *
 ********************************************************************************/

package builtInVMCode;

import Hack.VMEmulator.TerminateVMProgramThrowable;

/**
 * A built-in implementation for the Screen class of the Jack OS.
 */

@SuppressWarnings("UnusedDeclaration")
public class Jack_Screen extends JackOSClass {
    private static short color;

    public static void init() {
        color = (short)0xFFFF;
    }

    public static void clearScreen() throws TerminateVMProgramThrowable {
        for (int i = SCREEN_START_ADDRESS; i <= SCREEN_END_ADDRESS; ++i)
            writeMemory(i, 0);
    }

    private static void updateLocation(int address) throws TerminateVMProgramThrowable {
        writeMemory(SCREEN_START_ADDRESS + address, color);
    }

    public static void setColor(short r, short g, short b) {
        color = (short)(((r & 0xF8) << 8) | ((g & 0xFC) << 3) | (b >> 3) & 0xFFFF);
    }

    public static void drawPixel(short x, short y) throws TerminateVMProgramThrowable {
        if (x < 0 || x >= SCREEN_WIDTH || y < 0 || y >= SCREEN_HEIGHT)
            callFunction("Sys.error", SCREEN_DRAWPIXEL_ILLEGAL_COORDS);

        updateLocation(y * SCREEN_WIDTH + x);
    }

    private static void drawConditional(int x, int y, boolean exchange) throws TerminateVMProgramThrowable {
        if (exchange)
            updateLocation(x * SCREEN_WIDTH + y);
        else
            updateLocation(y * SCREEN_WIDTH + x);
    }

    public static void drawLine(short x1, short y1, short x2, short y2) throws TerminateVMProgramThrowable {
        if (x1 < 0 || x1 >= SCREEN_WIDTH || y1 < 0 || y1 >= SCREEN_HEIGHT ||
                x2 < 0 || x2 >= SCREEN_WIDTH || y2 < 0 || y2 >= SCREEN_HEIGHT) {
            callFunction("Sys.error", SCREEN_DRAWLINE_ILLEGAL_COORDS);
        }
        int dx = x2 - x1;
        if (dx < 0) dx = -dx;
        int dy = y2 - y1;
        if (dy < 0) dy = -dy;
        boolean loopOverY = (dx < dy);
        if ((loopOverY && (y2 < y1)) || ((!loopOverY) && (x2 < x1))) {
            short tmp = x1;
            x1 = x2;
            x2 = tmp;
            tmp = y1;
            y1 = y2;
            y2 = tmp;
        }
        int endX;
        int deltaY;
        int x, y;
        if (loopOverY) {
            int tmp = dx;
            dx = dy;
            dy = tmp;
            x = y1;
            y = x1;
            endX = y2;
            deltaY = (x1 > x2) ? -1 : 1;
        } else {
            x = x1;
            y = y1;
            endX = x2;
            deltaY = (y1 > y2) ? -1 : 1;
        }
        drawConditional(x, y, loopOverY);
        // var = 2*x*dy - 2*(|y|-0.5)*dx
        // ==> 	var >=0 iff 2*x*dy >= 2*(|y|-0.5)*dx
        // iff dy/dx >= x/(|y|-0.5)
        int var = 2 * dy - dx;
        int twody = 2 * dy;
        int twodyMinusTwodx = twody - 2 * dx;
        while (x < endX) {
            if (var < 0) {
                var += twody;
            } else {
                var += twodyMinusTwodx;
                y += deltaY;
            }
            ++x;
            drawConditional(x, y, loopOverY);
        }
    }

    public static void drawRectangle(short x1, short y1, short x2, short y2) throws TerminateVMProgramThrowable {
        if (x1 > x2 || y1 > y2 || x1 < 0 || x2 >= SCREEN_WIDTH ||
                y1 < 0 || y2 >= SCREEN_HEIGHT) {
            callFunction("Sys.error", SCREEN_DRAWRECTANGLE_ILLEGAL_COORDS);
        }
        for (int y = y1; y <= y2; y++) {
            int address = y * SCREEN_WIDTH;
            for (int x = x1; x <= x2; x++) {
              updateLocation(address + x);
            }
        }
    }

    private static void drawTwoHorizontal(int y1, int y2, int minX, int maxX) throws TerminateVMProgramThrowable {
        int address = y1 * SCREEN_WIDTH;
        for (int x = minX; x <= maxX; x++) {
          updateLocation(address + x);
        }
        address = y2 * SCREEN_WIDTH;
        for (int x = minX; x <= maxX; x++) {
          updateLocation(address + x);
        }
    }

    public static void drawCircle(short x, short y, short radius) throws TerminateVMProgramThrowable {
        if (x < 0 || x >= SCREEN_WIDTH || y < 0 || y >= SCREEN_HEIGHT) {
            callFunction("Sys.error", SCREEN_DRAWCIRCLE_ILLEGAL_CENTER);
        }
        if (x - radius < 0 || x + radius >= SCREEN_WIDTH ||
                y - radius < 0 || y + radius >= SCREEN_HEIGHT) {
            callFunction("Sys.error", SCREEN_DRAWCIRCLE_ILLEGAL_RADIUS);
        }
        int delta1 = 0;
        int delta2 = radius;
        int var = 1 - radius;
        drawTwoHorizontal(y - delta2, y + delta2, x - delta1, x + delta1);
        drawTwoHorizontal(y - delta1, y + delta1, x - delta2, x + delta2);
        while (delta2 > delta1) {
            if (var < 0) {
                var += 2 * delta1 + 3;
            } else {
                var += 2 * (delta1 - delta2) + 5;
                --delta2;
            }
            ++delta1;
            drawTwoHorizontal(y - delta2, y + delta2, x - delta1, x + delta1);
            drawTwoHorizontal(y - delta1, y + delta1, x - delta2, x + delta2);
        }
    }
}
