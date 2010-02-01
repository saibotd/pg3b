
package com.esotericsoftware.controller.device;

public abstract class Deadzone {
	protected float sizeX, sizeY;

	public float getSizeX () {
		return sizeX;
	}

	public void setSizeX (float sizeX) {
		this.sizeX = Math.abs(sizeX);
	}

	public float getSizeY () {
		return sizeY;
	}

	public void setSizeY (float sizeY) {
		this.sizeY = Math.abs(sizeY);
	}

	/**
	 * Converts the specified x and y percentage values to stick deflection values that ignore the deadzone. IE, non-zero values
	 * will return the first stick deflection in that direction that is outside the deadzone.
	 * @return Returns an 2 element array containing the x and y deflection values.
	 */
	abstract public float[] getOutput (float x, float y);

	/**
	 * Converts the specified x and y stick deflection values to percentage values that ignore the deadzone. IE, values that within
	 * the deadzone will return zero, values outside the deadzone will return the perctage between the edge of the deadzone and 1.
	 */
	abstract public float[] getInput (float x, float y);

	static public class Square extends Deadzone {
		public float[] getOutput (float x, float y) {
			float[] deflection = new float[2];
			deflection[0] = (1 - sizeX) * x + sizeX * Math.signum(x);
			deflection[1] = (1 - sizeY) * y + sizeY * Math.signum(y);
			if (deflection[0] != 0 || deflection[1] != 0) System.out.println("output : " + deflection[0] + ", " + deflection[1]);
			return deflection;
		}

		public float[] getInput (float x, float y) {
			float[] deflection = new float[2];
			float absX = Math.abs(x);
			float absY = Math.abs(y);
			deflection[0] = absX < sizeX ? 0 : (absX - sizeX) / (1 - sizeX) * Math.signum(x);
			deflection[1] = absY < sizeY ? 0 : (absY - sizeY) / (1 - sizeY) * Math.signum(y);
			if (deflection[0] != 0 || deflection[1] != 0) System.out.println("input : " + deflection[0] + ", " + deflection[1]);
			return deflection;
		}
	}

	static public class Round extends Deadzone {
		public float[] getOutput (float x, float y) {
			float[] deflection = new float[2];
			if (x != 0 || y != 0) {
				double angle = Math.atan2(y, x);
				float sin = (float)Math.sin(angle);
				float cos = (float)Math.cos(angle);

				float deadzoneX = sizeY * cos;
				float deadzoneY = sizeX * sin;
				float deadzoneDist = sizeX * sizeY / (float)Math.sqrt(deadzoneX * deadzoneX + deadzoneY * deadzoneY);

				float maxDist;
				if (Math.abs(x) > Math.abs(y))
					maxDist = 1 / cos * Math.signum(x);
				else
					maxDist = 1 / sin * Math.signum(y);

				float factor = (maxDist - deadzoneDist) / maxDist;
				x *= factor;
				y *= factor;
				float factoredDist = (float)Math.sqrt(x * x + y * y);
				float finalDist = factoredDist + deadzoneDist;

				deflection[0] = finalDist * cos;
				deflection[1] = finalDist * sin;
			}
			return deflection;
		}

		public float[] getInput (float x, float y) {
			float[] deflection = new float[2];
			if (x != 0 || y != 0) {
				float angle = (float)Math.atan2(y, x);
				float sin = (float)Math.sin(angle);
				float cos = (float)Math.cos(angle);

				float deadzoneX = sizeY * cos;
				float deadzoneY = sizeX * sin;
				float deadzoneDist = sizeX * sizeY / (float)Math.sqrt(deadzoneX * deadzoneX + deadzoneY * deadzoneY);

				float dist = (float)Math.sqrt(x * x + y * y);
				if (dist < Math.abs(deadzoneDist)) return deflection;

				float maxDist, nx, ny;
				if (Math.abs(x) > Math.abs(y))
					maxDist = 1 / cos * Math.signum(x);
				else
					maxDist = 1 / sin * Math.signum(y);

				float percent = (dist - deadzoneDist) / (maxDist - deadzoneDist);
				deflection[0] = maxDist * cos * percent;
				deflection[1] = maxDist * sin * percent;
			}
			return deflection;
		}
	}
}
