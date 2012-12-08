package chip;

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

  /**
   * Reset the Chip 8 memory and pointers
   */
  public void init() {
    memory = new char[4096];
    V = new char[16];
    I = 0x0;

    stack = new char[16];
    stackPointer = 0;

    delay_timer = 0;
    sound_timer = 0;

    keys = new byte[16];

    display = new byte[64 * 32];
  }

}
