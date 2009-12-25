
package com.esotericsoftware.controller.device;

public abstract class Deadzone {
	protected float sizeX, sizeY;

	public float getSizeX () {
		return sizeX;
	}

	public void setSizeX (float sizeX) {
		this.sizeX = sizeX;
	}

	public float getSizeY () {
		return sizeY;
	}

	public void setSizeY (float sizeY) {
		this.sizeY = sizeY;
	}

	/**
	 * Converts the specified x and y values to stick deflection values that ignore the deadzone. IE, non-zero values will return
	 * the first stick deflection in that direction that is outside the deadzone.
	 * @return Returns an 2 element array containing the x and y deflection values.
	 */
	abstract public float[] toDeflection (float x, float y);

	static public class Square extends Deadzone {
		public float[] toDeflection (float x, float y) {
			float[] deflection = new float[2];
			deflection[0] = (1 - sizeX) * x + sizeX * Math.signum(x);
			deflection[1] = (1 - sizeY) * y + sizeY * Math.signum(y);
			return deflection;
		}
	}

	static public class Round extends Deadzone {
		public float[] toDeflection (float x, float y) {
			float[] deflection = new float[2];
			if (x != 0 || y != 0) {
				double angle = Math.atan2(y, x);
				float xMax, yMax;
				if (Math.abs(x) > Math.abs(y)) {
					xMax = Math.signum(x);
					yMax = y * x / xMax;
				} else {
					yMax = Math.signum(y);
					xMax = x * y / yMax;
				}
				float maxDist = (float)Math.sqrt(xMax * xMax + yMax * yMax);
				float deadzoneX = sizeX * (float)Math.sin(angle);
				float deadzoneY = sizeY * (float)Math.cos(angle);
				float deadzoneDist = sizeX * sizeY / (float)Math.sqrt(deadzoneX * deadzoneX + deadzoneY * deadzoneY);
				float factor = (maxDist - deadzoneDist) / maxDist;
				x *= factor;
				y *= factor;
				float factoredDist = (float)Math.sqrt(x * x + y * y);
				float finalDist = factoredDist + deadzoneDist;
				deflection[0] = finalDist * (float)Math.cos(angle);
				deflection[1] = finalDist * (float)Math.sin(angle);
			}
			return deflection;
		}
	}
}
