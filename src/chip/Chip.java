package chip;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

public class Chip {

    /**
     * 4kB of 8-bit memory
     * At position 0x50: The "bios" fontset
     * At position 0x200: The start of every program
     */
    private char[] memory;
    /**
     * 16 8-bit registers.
     * They will be used to store data which is used in several operation
     * Register 0xF is used for Carry, Borrow and collision detection
     */
    private char[] V;
    /**
     * 16-bit (only 12 are used) to point to a specific point in the memory
     */
    private char I;
    /**
     * The 16-bit (only 12 are used) to point to the current operation
     */
    private char pc;

    /**
     * Subroutine callstack
     * Allows up to 16 levels of nesting
     */
    private char[] stack;
    /**
     * Points to the next free slot int the stack
     */
    private int stackPointer;

    /**
     * This timer is used to delay events in programs/games
     */
    private int delay_timer;
    /**
     * This timer is used to make a beeping sound
     */
    private int sound_timer;

    /**
     * This array will be our keyboard state
     */
    private byte[] keys;

    /**
     * The 64x32 pixel monochrome (black/white) display
     */
    private byte[] display;

    private boolean needRedraw;

    /**
     * Reset the Chip 8 memory and pointers
     */
    public void init() {
        memory = new char[4096];
        V = new char[16];
        I = 0x0;
        pc = 0x200;

        stack = new char[16];
        stackPointer = 0;

        delay_timer = 0;
        sound_timer = 0;

        keys = new byte[16];

        display = new byte[64 * 32];

        needRedraw = false;
        loadFontset();
    }

    /**
     * Executes a single Operation Code (Opcode)
     */
    public void run() {
        // fetch Opcode
        char opcode = (char) ((memory[pc] << 8) | memory[pc + 1]);
        System.out.print(Integer.toHexString(opcode).toUpperCase() + ": ");
        // decode Opcode
        // execute Opcode
        switch (opcode & 0xF000) {

            case 0x0000: { // Multi-case
                switch(opcode & 0x0FFF) {
                    case 0x00E0: { // 00E0: Clear Screen
                        for (int i = 0; i < display.length; i++){
                            display[i] = 0;
                        }
                        pc += 0x2;
                        break;
                    }

                    case 0x00EE: { // 00EE: Returns from subroutine
                        stackPointer--;
                        pc = (char)(stack[stackPointer] + 2);
                        System.out.println("Returning to 0x" + Integer.toHexString(pc).toUpperCase());
                        break;
                    }

                    default: { // 0NNN: Calls RCA 1802 Program at address NNN
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                        break;
                    }

                }
                break;

            }

            case 0x1000: { // 1NNN: Jumps to address NNN
                char nnn = (char)(opcode & 0x0FFF);
                pc = nnn;
                System.out.println("Jumping to 0x" + Integer.toHexString(nnn).toUpperCase());
                break;
            }

            case 0x2000: { // 2NNN: Calls subroutine at NNN
                stack[stackPointer] = pc;
                stackPointer++;
                pc = (char)(opcode & 0x0FFF);
                System.out.println("Calling 0x" + Integer.toHexString(pc).toUpperCase() + " from " + Integer.toHexString(stack[stackPointer - 1]).toUpperCase());
                break;
            }

            case 0x3000: { // 3XNN: Skips the next instruction if VX equals NN
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                System.out.print("Skips if V[0x" + Integer.toHexString(x).toUpperCase() + "] == 0x" + Integer.toHexString(nn).toUpperCase() +". ");
                if (V[x] == nn) {
                    pc += 0x4;
                    System.out.println("Skipping next instruction (V[0x" + Integer.toHexString(x).toUpperCase() +"] == 0x" + Integer.toHexString(nn).toUpperCase() + ")");
                }
                else {
                    pc += 0x2;
                    System.out.println("Not skipping next instruction (V[0x" + Integer.toHexString(x).toUpperCase() +"] != 0x" + Integer.toHexString(nn).toUpperCase() + ")");
                }
                break;
            }

            case 0x4000: { // 4XNN: Skips the next instruction if VX != NN
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                System.out.print("Skips if V[0x" + Integer.toHexString(x).toUpperCase() + "] != 0x" + Integer.toHexString(nn).toUpperCase() +". ");
                if (V[x] != nn){
                    pc += 0x4;
                    System.out.println("0x" + Integer.toHexString(V[x]).toUpperCase() + " != 0x" + Integer.toHexString(nn).toUpperCase() +". Skipping next instruction.");
                }
                else {
                    pc += 0x2;
                    System.out.println("0x" + Integer.toHexString(V[x]).toUpperCase() + " == 0x" + Integer.toHexString(nn).toUpperCase() +". Not skipping next instruction.");
                }
                break;
            }

            case 0x5000: { // 5XY0: Skips next instruction if VX == VY
                int x = (opcode & 0x0F00) >> 8;
                int y = (opcode & 0X00F0) >> 4;
                System.out.print("Skips if V[0x" + Integer.toHexString(x).toUpperCase() + "] != V[0x" + Integer.toHexString(y).toUpperCase() +". ");
                if (V[x] == V[y]) {
                    pc += 0x4;
                    System.out.println("0x" + Integer.toHexString(V[x]).toUpperCase() + " == 0x" + Integer.toHexString(V[y]).toUpperCase() +". Skipping next instruction.");
                }
                else {
                    pc += 0x2;
                    System.out.println("0x" + Integer.toHexString(V[x]).toUpperCase() + " != 0x" + Integer.toHexString(V[y]).toUpperCase() +". Not skipping next instruction.");
                }
                break;
            }

            case 0x6000: { // 6XNN: Sets VX to NN
                int x = ((opcode & 0x0F00) >> 8);
                int nn = (opcode & 0x00FF);
                V[x] = (char)nn;
                pc += 0x2; // advanced 2 because opcode uses pc and pc+1
                System.out.println("Setting V[0x" + Integer.toHexString(x).toUpperCase() + "] to 0x" + Integer.toHexString(nn).toUpperCase());
                break;
            }

            case 0x7000: { // 7XNN: Adds NN to VX
                int x = ((opcode & 0x0F00) >> 8);
                int nn = (opcode & 0x00FF);
                V[x] = (char)((V[x] + nn) & 0xFF);
                pc += 0x2;
                System.out.println("Adding 0x" + Integer.toHexString(nn).toUpperCase() + " to V[0x" + Integer.toHexString(x).toUpperCase() + "] = 0x" + Integer.toHexString(V[x]).toUpperCase());
                break;
            }

            case 0x8000: { // Contains more data in last nibble

                switch (opcode & 0x000F) {

                    case 0x0000: { // 8XY0: Sets VX to the value of VY.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        V[x] = V[y];
                        pc += 0x2;
                        System.out.println("Sets V[0x" + Integer.toHexString(x).toUpperCase() + "] to V[0x" + Integer.toHexString(y).toUpperCase() + "] = 0x" + Integer.toHexString(V[y]).toUpperCase());
                        break;
                    }

                    case 0x0001: { // 8XY2: Sets VX to (VX OR VY)
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        V[x] = (char)((V[x] | V[y]) & 0xFF);
                        pc += 0x2;
                        System.out.println("Sets V[0x" + Integer.toHexString(x).toUpperCase() + "] to 0x" + Integer.toHexString(V[x]).toUpperCase() + " | 0x" + Integer.toHexString(V[y]).toUpperCase() + " = 0x" + Integer.toHexString(V[x] | V[y]).toUpperCase());
                        break;
                    }

                    case 0x0002: { // 8XY2: Sets VX to (VX AND VY)
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        V[x] = (char)((V[x] & V[y]) & 0xFF);
                        pc += 0x2;
                        System.out.println("Sets V[0x" + Integer.toHexString(x).toUpperCase() + "] to 0x" + Integer.toHexString(V[x]).toUpperCase() + " & 0x" + Integer.toHexString(V[y]).toUpperCase() + " = 0x" + Integer.toHexString(V[x] & V[y]).toUpperCase());
                        break;
                    }

                    case 0x0003: { // 8XY2: Sets VX to (VX XOR VY)
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        V[x] = (char)((V[x] ^ V[y]) & 0xFF);
                        pc += 0x2;
                        System.out.println("Sets V[0x" + Integer.toHexString(x).toUpperCase() + "] to 0x" + Integer.toHexString(V[x]).toUpperCase() + " ^ 0x" + Integer.toHexString(V[y]).toUpperCase() + " = 0x" + Integer.toHexString(V[x] ^ V[y]).toUpperCase());
                        break;
                    }

                    case 0x0004: { // 8XY4: Adds VY to VX.  VF is set to 1 when carry applies
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        int result = V[x] + V[y];
                        V[0xF] = (char)((result & 0x100) >> 8);

                        if(V[0xF]==1){
                            System.out.print("Carry Flag raised. ");
                        }
                        else {
                            System.out.print("Carry Flag cleared. ");
                        }

                        V[x] = (char)(result & 0xFF);
                        pc += 0x2;
                        System.out.println("Adds V[0x" + Integer.toHexString(x).toUpperCase() + "] and V[0x" + Integer.toHexString(y).toUpperCase() + "] = " + Integer.toHexString(result).toUpperCase());
                        break;
                    }

                    case 0x0005: { // 8XY5: Set Vx = Vx - Vy, set VF = NOT borrow.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        if (V[x] > V[y]){
                            V[0xF] = 1;
                            System.out.print("NOT Borrow Flag raised. ");
                        }
                        else {
                            V[0xF] = 0;
                            System.out.print("NOT Borrow Flag cleared. ");
                        }
                        V[x] = (char)((V[x] - V[y]) & 0xFF);
                        pc += 0x2;
                        System.out.println("Subtracting V[0x" + Integer.toHexString(y).toUpperCase() + "] from V[0x" + Integer.toHexString(x).toUpperCase() + ". Result = 0x" + Integer.toHexString(V[x]).toUpperCase());
                        break;
                    }


                    case 0x0006: { // 8XY6: Set Vx = Vx SHR 1.
                        int x = (opcode & 0x0F00) >> 8;
                        // int y = (opcode & 0x00F0) >> 4;
                        if ((V[x] & 0x1)==1){
                            V[0xF] = 1;
                            System.out.print("Overflow Flag raised. ");
                        }
                        else {
                            V[0xF] = 0;
                            System.out.print("Overflow Flag cleared. ");
                        }
                        System.out.println("Dividing V[0x" + Integer.toHexString(x).toUpperCase() + "] = " + Integer.toHexString(V[x]).toUpperCase() + "by 2. Result = 0x" + Integer.toHexString(V[x]>>1).toUpperCase());
                        V[x] = (char)((V[x] >> 1) & 0xFF);
                        pc += 0x2;
                        break;
                    }


                    case 0x0007: { // 8007:
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                        break;
                    }


                    case 0x000E: { // 800E:
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                        break;
                    }

                    default: {
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                        break;
                    }

                }

                break;
            }

            case 0x9000: { // 9XY0: Skip next instruction if Vx != Vy.
                int x = (opcode & 0x0F00) >> 8;
                int y = (opcode & 0x00F0) >> 4;
                System.out.print("Skips if V[0x" + Integer.toHexString(x).toUpperCase() + "] != V[0x" + Integer.toHexString(y).toUpperCase() +"]. ");
                if (V[x] != V[y]){
                    pc += 0x4;
                    System.out.println("0x" + Integer.toHexString(V[x]).toUpperCase() + " != 0x" + Integer.toHexString(V[y]).toUpperCase() +". Skipping next instruction.");
                }
                else {
                    pc += 0x2;
                    System.out.println("0x" + Integer.toHexString(V[x]).toUpperCase() + " == 0x" + Integer.toHexString(V[y]).toUpperCase() +". Not skipping next instruction.");
                }
                break;
            }

            case 0xA000: { // ANNN: Sets I to NNN
                int nnn = (opcode & 0x0FFF);
                I = (char)nnn;
                pc += 0x2;
                System.out.println("Set I to " + Integer.toHexString(nnn).toUpperCase());
                break;
            }

            case 0xC000: { // CXNN: Sets VX to a random number AND NN
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                int randomNumber =  new Random().nextInt(256) & nn;
                V[x] = (char)(randomNumber & 0xFF);
                pc += 0x2;
                System.out.println("V[0x" + Integer.toHexString(x).toUpperCase() + "] has been set to (randomised) 0x" + Integer.toHexString(randomNumber).toUpperCase());
                break;
            }

            case 0xD000: { // DXYN: Draws a sprite (V[X], V[Y]) size(8, N). Sprite is located at I
                // Drawing by XOR-ing to the screen
                // Check collision and set V[0xF]
                // Read the image from I
                int x = V[(opcode & 0x0F00) >> 8];
                int y = V[(opcode & 0x00F0) >> 4];
                int height = opcode & 0x000F;

                V[0xF] = 0;

                for(int _y = 0; _y < height; _y++) {
                    int line = memory[I + _y];
                    for(int _x = 0; _x < 8; _x++) {
                        int pixel = line & (0x80 >> _x);
                        if(pixel != 0) {
                            int totalX = x + _x;
                            int totalY = y + _y;
                            // If the sprite is positioned so part of it is outside the coordinates of the display,
                            // it wraps around to the opposite side of the screen
                            //index %= 64 * 32;
                            totalX %= 64;
                            totalY %= 32;
                            int index = totalY * 64 + totalX;

                            //System.out.print("Index = " + index + ". ");
                            if(display[index] == 1)
                                V[0xF] = 1;

                            display[index] ^= 1;
                        }
                    }
                }
                pc += 0x2;
                needRedraw = true;
                System.out.println("Drawing at V[0x" + Integer.toHexString((opcode & 0x0F00) >> 8).toUpperCase() + "] = 0x" + Integer.toHexString(x).toUpperCase() + ", V[0x" + Integer.toHexString((opcode & 0x00F0) >> 4).toUpperCase() + "] = 0x" + Integer.toHexString(y).toUpperCase());
                break;
            }

            case 0xE000: { // Multi-case
                switch (opcode & 0x00FF){

                    case 0x009E: { // EX9E: Skips the next instruction if key VX is pressed
                        int x = (opcode & 0x0F00) >> 8;
                        int key = V[x];
                        if (keys[key] == 1) {
                            pc += 0x4;
                        }
                        else {
                            pc += 0x2;
                        }
                        break;
                    }

                    case 0x00A1: { // EXA1: Skips the next instruction if the key VX is not pressed
                        int x = (opcode & 0x0F00) >> 8;
                        int key = V[x];
                        if (keys[key] == 0) {
                            pc += 0x4;
                        }
                        else {
                            pc += 0x2;
                        }
                        break;
                    }

                    default: {
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                    }
                }
                break;
            }

            case 0xF000: { // Multi-case

                switch (opcode & 0x00FF) {

                    case 0x0007: { // FX07: Sets VX to the value of delay_timer
                        int x = (opcode & 0x0F00) >> 8;
                        V[x] = (char)delay_timer;
                        pc += 0x2;
                        System.out.println("V[0x" + Integer.toHexString(x).toUpperCase() + "] has been set to delay_timer = 0x" + Integer.toHexString(delay_timer).toUpperCase());
                        break;
                    }

                    case 0x000A: { // FX0A: Wait for key press, store the value of the key in VX
                        int x = (opcode & 0x0F00) >> 8;
                        System.out.print("Waiting for keypress. ");
                        for (int i = 0; i < keys.length; i++)  {
                            if (keys[i] == 1) {
                                pc += 0x2;
                                V[x] = (char)i;
                                System.out.print("Found: " + Integer.toHexString(i).toUpperCase() + "! ");
                                return;
                            }
                        }
                        System.out.println();
                        break;
                    }

                    case 0x0015: { // FX15: Sets delay time to VX
                        int x = (opcode & 0x0F00) >> 8;
                        delay_timer = V[x];
                        pc += 0x2;
                        System.out.println("Sets delay_timer to V[0x" + Integer.toHexString(x).toUpperCase() + "] = 0x" + Integer.toHexString(V[x]).toUpperCase());
                        break;
                    }

                    case 0x0018: { // FX18: Set sound timer = Vx
                        int x = (opcode & 0x0F00) >> 8;
                        sound_timer = V[x];
                        pc += 0x2;
                        System.out.println("Sets sound_timer to V[0x" + Integer.toHexString(x).toUpperCase() + "] = 0x" + Integer.toHexString(V[x]).toUpperCase());
                        break;
                    }

                    case 0x001E: { // FX1E: Set I = I + Vx.
                        int x = (opcode & 0x0F00) >> 8;
                        I = (char)((I + V[x]) & 0xFFFF);
                        pc += 0x2;
                        System.out.println("Adding V[" + Integer.toHexString(x).toUpperCase() + "] to I. I = " + Integer.toHexString(I).toUpperCase());
                        break;
                    }

                    case 0x0029: { // FX29: Sets I to the location of the sprite for the character VX (Fontset)
                        int x = (opcode & 0x0F00) >> 8;
                        int character = V[x];
                        I = (char)(0x50 + (character * 5));
                        System.out.println("Setting I to Character V[0x" + Integer.toHexString(x).toUpperCase() + "] = 0x" + Integer.toHexString(V[x]).toUpperCase() + " Offset to 0x" + Integer.toHexString(I).toUpperCase());
                        pc += 0x2;
                        break;
                    }

                    case 0x0033: { // FX33: Store a binary-coded decimal value VX in I, I + 1 and I + 2
                        int x = (opcode & 0x0F00) >> 8;
                        int value = V[x];
                        int hundreds = (value - (value % 100)) / 100;
                        value -= hundreds * 100;
                        int tens = (value - (value % 10)) / 10;
                        value -= tens * 10;
                        memory[I] = (char)hundreds;
                        memory[I + 1] = (char)tens;
                        memory[I + 2] = (char)value;
                        pc += 0x2;
                        System.out.println("Storing Binary-Coded Decimal V[0x" + Integer.toHexString(x).toUpperCase() + "] = " + Integer.toHexString(V[x]).toUpperCase() + " as { " + hundreds + ", " + tens + ", " + value + "}");
                        break;
                    }

                    case 0x0055: {
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                        break;
                    }

                    case 0x0065: { // FX65: Fills V0 to VX with values from I
                        int x = (opcode & 0x0F00) >> 8;
                        for (int i = 0; i <= x; i++) {
                            V[i] = memory[I + i];
                        }
                        pc += 0x2;
                        System.out.println("Setting V[0x0] to V[0x" + Integer.toHexString(x).toUpperCase() + "] to the values of memory[0x" + Integer.toHexString(I & 0xFFFF).toUpperCase() + "]");
                        break;
                    }
                    default: {
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                    }
                }
                break;
            }

            default: {
                System.err.println("Unsupported Opcode!");
                System.exit(0);
            }
        }

        // The delay timer is active whenever the delay timer register (DT) is non-zero.
        // This timer does nothing more than subtract 1 from the value of DT at a rate of 60Hz.
        // When DT reaches 0, it deactivates.
        if (delay_timer != 0) {
            delay_timer = (char)(delay_timer - 1);
        }

        if (sound_timer != 0) {
            sound_timer = (char)(sound_timer -1);
            Audio.playSound("./beep.wav");
        }
    }

    /**
     * Returns the display data
     * @return
     * Current state of the 64x32 display
     */
    public byte[] getDisplay() {
        return display;
    }

    public boolean needsRedraw() {
        return needRedraw;
    }

    public void removeDrawFlag() {
        needRedraw = false;
    }

    public void loadProgram(String file) {
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(new File(file)));

            int offset = 0;
            while(input.available() > 0) {
                memory[0x200 + offset] = (char)(input.readByte() & 0xFF);
                offset++;
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {}
            }
        }
    }

    /**
     * Loads the fontset into the memory
     */
    public void loadFontset() {
        for(int i=0; i < ChipData.fontset.length; i++) {
            memory[0x50 + i] = ChipData.fontset[i];
        }
    }


    public void setKeyBuffer(int[] keyBuffer) {
        for(int i = 0; i < keys.length; i++) {
            keys[i] = (byte)keyBuffer[i];
//            System.out.println(i + ": " + keys[i]);
        }
    }
}
