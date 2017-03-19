package evolv.io;

import java.util.ArrayList;

import processing.core.PFont;

public class Creature extends SoftBody implements java.io.Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 476572589771857421L;
	/**
	 * 
	 */
	// Energy
	double ACCELERATION_ENERGY = 0.18f;
	double ACCELERATION_BACK_ENERGY = 0.24f;
	double SWIM_ENERGY = 0.008f;
	//double TURN_ENERGY = 0.06f;
	double TURN_ENERGY = 1f;
	double EAT_ENERGY = 0.05f;
	double EAT_SPEED = 0.5f; // 1 is instant, 0 is nonexistent, 0.001 is
								// verrry slow.
	double EAT_WHILE_MOVING_INEFFICIENCY_MULTIPLIER = 2.0f; // The bigger
															// this number
															// is, the less
															// effiently
															// creatures eat
															// when they're
															// moving.
	double FIGHT_ENERGY = 0.06f;
	double INJURED_ENERGY = 0.25f;
	double METABOLISM_ENERGY = 0.004f;
	double AGE_FACTOR = 1; // 0 no ageing
	double currentEnergy;
	final int ENERGY_HISTORY_LENGTH = 6;
	double[] previousEnergy = new double[ENERGY_HISTORY_LENGTH];

	// Family
	String name;
	String parents;
	int gen;
	int mut;
	int id;
	int numChildren = 0;
	public static int NUM_CHILDREN_BEFORE_MUTATING = 2;

	// Vision or View or Preference
	double MAX_VISION_DISTANCE = 10;
	final double FOOD_SENSITIVITY = 0.3f;
	public static int EYE_COUNT = 2;
	double[] visionAngles = new double[EYE_COUNT];
	double[] visionDistances = new double[EYE_COUNT];
	// double visionAngle;
	// double visionDistance;
	double[] visionOccludedX = new double[EYE_COUNT];
	double[] visionOccludedY = new double[EYE_COUNT];
	double visionResults[] = new double[EYE_COUNT*3];
	boolean visionDistanceIsBrainControlled = true;
	boolean visionAngleIsBrainControlled = false;
	//double[] customAngles = {0, -0.2, 0.2, -0.3, 0.3, -0.4, 0.4};
	//double[] customDistances = {0, 0.5, 0.5, 0.6, 0.6, 0.5, 0.5};
	
	/*
	double[] customAngles = { 0, 0f, 0f };
	double[] customDistances = { 0, 0.3f, 0.7f };
	*/
	
	double[] customAngles = { 0, 0f };
	double[] customDistances = { 0, 0.4f };

	Brain brain;
	final float BRIGHTNESS_THRESHOLD = 0.7f;

	// Misc or Unsorted
	float preferredRank = 8;
	float CROSS_SIZE = 0.022f;
	double mouthHue;
	double vr = 0;
	double rotation = 0;
	final double SAFE_SIZE = 1.25f;
	//final double MATURE_AGE = 0.01f;
	final double MATURE_AGE = 0f;

	NameGenerator nameGenerator;

	public Creature(Board tb) {
		this(tb.evolvioColor.random(tb.evolvioColor.cameraX - tb.boardWidth/tb.evolvioColor.zoom, tb.evolvioColor.cameraX + tb.boardWidth/tb.evolvioColor.zoom),
				tb.evolvioColor.random(tb.evolvioColor.cameraY - tb.boardHeight/tb.evolvioColor.zoom, tb.evolvioColor.cameraY + tb.boardHeight/tb.evolvioColor.zoom), 0, 0,
				tb.evolvioColor.random(tb.MIN_CREATURE_ENERGY, tb.MAX_CREATURE_ENERGY), 1,
				tb.evolvioColor.random(0, 1), 1, 1, tb,
				tb.evolvioColor.random(0, 2 * EvolvioColor.PI), 0, "", "[PRIMORDIAL]", true, null, 1,
				tb.evolvioColor.random(0, 1), 0);
	}

	public Creature(double tpx, double tpy, double tvx, double tvy, double tenergy,
			double tdensity, double thue, double tsaturation, double tbrightness, Board tb, double rot,
			double tvr, String tname, String tparents, boolean mutateName, Brain brain, int tgen, double tmouthHue, int tmut) {

		super(tpx, tpy, tvx, tvy, tenergy, tdensity, thue, tsaturation, tbrightness, tb);
		nameGenerator = new NameGenerator(tb.evolvioColor);

		if (brain == null)
			brain = new Brain(tb, null, null);
		this.brain = brain;

		rotation = rot;
		vr = tvr;
		isCreature = true;
		id = board.creatureIDUpTo + 1;
		if (tname.length() >= 1) {
			if (mutateName) {
				name = nameGenerator.mutateName(tname);
			} else {
				name = tname;
			}
			name = nameGenerator.sanitizeName(name);
		} else {
			name = nameGenerator.newName();
		}
		parents = tparents;
		board.creatureIDUpTo++;
		// visionAngle = 0;
		// visionDistance = 0;
		// visionEndX = getVisionStartX();
		// visionEndY = getVisionStartY();
		for (int i = 0; i < EYE_COUNT*3; i++) {
			visionResults[i] = 0;
		}
		gen = tgen;
		mut = tmut;
		mouthHue = tmouthHue;
	}

	public void drawBrain(PFont font, float scaleUp, int mX, int mY) {
		brain.draw(font, scaleUp, mX, mY);
	}

	public void useBrain(double timeStep, boolean useOutput) {
		double inputs[] = new double[brain.BASE_HEIGHT];
		for (int i = 0; i < EYE_COUNT*3; i++) {
			inputs[i] = visionResults[i];
		}
		
		inputs[EYE_COUNT*3] = energy;
		inputs[EYE_COUNT*3 + 1] = mouthHue;
		//inputs[EYE_COUNT*3 + 2] = board.year - birthTime;
		brain.input(inputs);
		if (useOutput) {
			double[] output = brain.outputs();
			hue = Math.abs(output[0]) % 1.0f;
			accelerate(output[1], timeStep);
			turn(output[2], timeStep);
			eat(output[3], timeStep);
			fight(output[4], timeStep * 100);
			if (output[5] > 0 && board.year - birthTime >= MATURE_AGE && energy > SAFE_SIZE) {
				reproduce(SAFE_SIZE, timeStep);
			}
			mouthHue = Math.abs(output[6]) % 1.0f;
			
			if(visionDistanceIsBrainControlled && visionAngleIsBrainControlled){
				for(int i = 2; i < EYE_COUNT*2; i+=2){
					//visionAngles[i/2] = Brain.activationFunction(output[5+i]*2*Math.PI);
					//visionDistances[i/2] = Brain.activationFunction(output[6+i]);
					visionDistances[i/2] = output[6+i];
					visionAngles[i/2] = output[5+i];
				}
			}
			else if(visionDistanceIsBrainControlled){
				for(int i = 2; i < EYE_COUNT*2; i+=2){
					visionDistances[i/2] = output[6+i];
				}
				visionAngles = customAngles;
			}
			else if(visionAngleIsBrainControlled){
				for(int i = 2; i < EYE_COUNT*2; i+=2){
					visionAngles[i/2] = output[5+i];
				}
				visionDistances = customDistances;
			}
			else{

				EYE_COUNT = customAngles.length;
				visionAngles = customAngles;
				visionDistances = customDistances;
			}
			
			/*
			visionAngles[1] = Brain.activationFunction(output[6]);
			visionDistances[1] = Brain.activationFunction(output[7]);
			visionAngles[2] = Brain.activationFunction(output[8]);
			visionDistances[2] = Brain.activationFunction(output[9]);
			visionAngles[3] = Brain.activationFunction(output[11]);
			visionDistances[3] = Brain.activationFunction(output[12]);
			visionAngles[4] = Brain.activationFunction(output[13]);
			visionDistances[4] = Brain.activationFunction(output[14]);
			*/
			
			/*
			visionAngles[1] = output[6];
			visionDistances[1] = output[7];
			visionAngles[2] = output[8];
			visionDistances[2] = output[9];
			visionAngles[3] = output[11];
			visionDistances[3] = output[12];
			visionAngles[4] = output[13];
			visionDistances[4] = output[14];
			*/
			/*
			visionAngles[1] = Math.sin(output[6]);
			visionDistances[1] = Math.sin(output[7]);
			visionAngles[2] = Math.sin(output[8]);
			visionDistances[2] = Math.sin(output[9]);
			visionAngles[3] = Math.sin(output[11]);
			visionDistances[3] = Math.sin(output[12]);
			visionAngles[4] = Math.sin(output[13]);
			visionDistances[4] = Math.sin(output[14]);
			*/
			/*
			visionAngles[1] = Math.sin(output[6]/50);
			visionDistances[1] = Math.sin(output[7]/50);
			visionAngles[2] = Math.sin(output[8]/50);
			visionDistances[2] = Math.sin(output[9]/50);
			visionAngles[3] = Math.sin(output[11]/50);
			visionDistances[3] = Math.sin(output[12]/50);
			visionAngles[4] = Math.sin(output[13]/50);
			visionDistances[4] = Math.sin(output[14]/50);
			*/
		}
	}

	public void drawSoftBody(float scaleUp, float camZoom, boolean showVision) {
		board.evolvioColor.ellipseMode(EvolvioColor.RADIUS);
		double radius = getRadius();
		if (showVision && camZoom > Board.MAX_DETAILED_ZOOM) {
			drawVisionAngles(board, scaleUp);
		}
		board.evolvioColor.noStroke();
		if (fightLevel > 0) {
			board.evolvioColor.fill(0, 1, 1, (float) (fightLevel * 0.8f));
			board.evolvioColor.ellipse((float) (px * scaleUp), (float) (py * scaleUp),
					(float) (FIGHT_RANGE * radius * scaleUp), (float) (FIGHT_RANGE * radius * scaleUp));
		}
		board.evolvioColor.strokeWeight(board.CREATURE_STROKE_WEIGHT);
		board.evolvioColor.stroke(0, 0, 1);
		board.evolvioColor.fill(0, 0, 1);
		if (this == board.selectedCreature) {
			board.evolvioColor.ellipse((float) (px * scaleUp), (float) (py * scaleUp),
					(float) (radius * scaleUp + 1 + 75.0f / camZoom), (float) (radius * scaleUp + 1 + 75.0f / camZoom));
		}
		super.drawSoftBody(scaleUp);

		if (camZoom > Board.MAX_DETAILED_ZOOM) {
			drawMouth(board, scaleUp, radius, rotation, camZoom, mouthHue);
			if (showVision) {
				board.evolvioColor.fill(0, 0, 1);
				board.evolvioColor.textFont(board.evolvioColor.font, 0.2f * scaleUp);
				board.evolvioColor.textAlign(EvolvioColor.CENTER);
				board.evolvioColor.text(getCreatureName(), (float) (px * scaleUp),
						(float) ((py - getRadius() * 1.4f - 0.07f) * scaleUp));
			}
		}
	}

	public void drawVisionAngles(Board board, float scaleUp) {
		for (int i = 0; i < visionAngles.length; i++) {
			int visionUIcolor = board.evolvioColor.color(0, 0, 1);
			if (visionResults[i * 3 + 2] > BRIGHTNESS_THRESHOLD) {
				visionUIcolor = board.evolvioColor.color(0, 0, 0);
			}
			board.evolvioColor.stroke(visionUIcolor);
			board.evolvioColor.strokeWeight(board.CREATURE_STROKE_WEIGHT);
			float endX = (float) getVisionEndX(i);
			float endY = (float) getVisionEndY(i);
			board.evolvioColor.line((float) (px * scaleUp), (float) (py * scaleUp), endX * scaleUp, endY * scaleUp);
			board.evolvioColor.noStroke();
			board.evolvioColor.fill(visionUIcolor);
			board.evolvioColor.ellipse((float) (visionOccludedX[i] * scaleUp), (float) (visionOccludedY[i] * scaleUp),
					2 * CROSS_SIZE * scaleUp, 2 * CROSS_SIZE * scaleUp);
			board.evolvioColor.stroke((float) (visionResults[i * 3]), (float) (visionResults[i * 3 + 1]),
					(float) (visionResults[i * 3 + 2]));
			board.evolvioColor.strokeWeight(board.CREATURE_STROKE_WEIGHT);
			board.evolvioColor.line((float) ((visionOccludedX[i] - CROSS_SIZE) * scaleUp),
					(float) ((visionOccludedY[i] - CROSS_SIZE) * scaleUp),
					(float) ((visionOccludedX[i] + CROSS_SIZE) * scaleUp),
					(float) ((visionOccludedY[i] + CROSS_SIZE) * scaleUp));
			board.evolvioColor.line((float) ((visionOccludedX[i] - CROSS_SIZE) * scaleUp),
					(float) ((visionOccludedY[i] + CROSS_SIZE) * scaleUp),
					(float) ((visionOccludedX[i] + CROSS_SIZE) * scaleUp),
					(float) ((visionOccludedY[i] - CROSS_SIZE) * scaleUp));
		}
	}

	public void drawMouth(Board board, float scaleUp, double radius, double rotation, float camZoom, double mouthHue) {
		board.evolvioColor.noFill();
		board.evolvioColor.strokeWeight(board.CREATURE_STROKE_WEIGHT);
		board.evolvioColor.stroke(0, 0, 1);
		board.evolvioColor.ellipseMode(EvolvioColor.RADIUS);
		board.evolvioColor.ellipse((float) (px * scaleUp), (float) (py * scaleUp),
				board.MINIMUM_SURVIVABLE_SIZE * scaleUp, board.MINIMUM_SURVIVABLE_SIZE * scaleUp);
		board.evolvioColor.pushMatrix();
		board.evolvioColor.translate((float) (px * scaleUp), (float) (py * scaleUp));
		board.evolvioColor.scale((float) radius);
		board.evolvioColor.rotate((float) rotation);
		board.evolvioColor.strokeWeight((float) (board.CREATURE_STROKE_WEIGHT / radius));
		board.evolvioColor.stroke(0, 0, 0);
		//System.out.println(this.name);
		board.evolvioColor.fill((float) mouthHue, 1.0f, 1.0f);
		board.evolvioColor.ellipse(0.6f * scaleUp, 0, 0.37f * scaleUp, 0.37f * scaleUp);
		board.evolvioColor.popMatrix();
	}

	public void metabolize(double timeStep) {
		double age = AGE_FACTOR * (board.year - birthTime); // the older the
															// more work
															// necessary
		loseEnergy(energy * METABOLISM_ENERGY * age * timeStep);

		if (energy < SAFE_SIZE) {
			returnToEarth();
			board.creatures.remove(this);
		}
	}

	public void accelerate(double amount, double timeStep) {
		double multiplied = amount * timeStep / getMass();
		vx += Math.cos(rotation) * multiplied;
		vy += Math.sin(rotation) * multiplied;
		if (amount >= 0) {
			loseEnergy(amount * ACCELERATION_ENERGY * timeStep);
		} else {
			loseEnergy(Math.abs(amount * ACCELERATION_BACK_ENERGY * timeStep));
		}
	}

	public void turn(double amount, double timeStep) {
		vr += 0.04f * amount * timeStep / getMass();
		loseEnergy(Math.abs(amount * TURN_ENERGY * energy * timeStep));
	}

	public Tile getRandomCoveredTile() {
		double radius = (float) getRadius();
		double choiceX = 0;
		double choiceY = 0;
		while (EvolvioColor.dist((float) px, (float) py, (float) choiceX, (float) choiceY) > radius) {
			choiceX = (Math.random() * 2 * radius - radius) + px;
			choiceY = (Math.random() * 2 * radius - radius) + py;
		}
		int x = xBound((int) choiceX);
		int y = yBound((int) choiceY);
		return board.tiles[x][y];
	}

	public void eat(double attemptedAmount, double timeStep) {
		double amount = attemptedAmount / (1.0f + distance(0, 0, vx, vy) * EAT_WHILE_MOVING_INEFFICIENCY_MULTIPLIER); // The
																														// faster
																														// you're
																														// moving,
																														// the
																														// less
																														// efficiently
																														// you
																														// can
																														// eat.
		if (amount < 0) {
			dropEnergy(-amount * timeStep);
			loseEnergy(-attemptedAmount * EAT_ENERGY * timeStep);
		} else {
			Tile coveredTile = getRandomCoveredTile();
			double foodToEat = coveredTile.foodLevel * (1 - Math.pow((1 - EAT_SPEED), amount * timeStep));
			if (foodToEat > coveredTile.foodLevel) {
				foodToEat = coveredTile.foodLevel;
			}
			coveredTile.removeFood(foodToEat, true);
			double foodDistance = Math.abs(coveredTile.foodType - mouthHue);
			double multiplier = 1.0f - foodDistance / FOOD_SENSITIVITY;
			if (multiplier >= 0) {
				addEnergy(foodToEat * multiplier);
			} else {
				loseEnergy(-foodToEat * multiplier);
			}
			loseEnergy(attemptedAmount * EAT_ENERGY * timeStep);
		}
	}

	public void fight(double amount, double timeStep) {
		if (amount > 0 && board.year - birthTime >= MATURE_AGE) {
			fightLevel = amount;
			loseEnergy(fightLevel * FIGHT_ENERGY * energy * timeStep);
			for (int i = 0; i < colliders.size(); i++) {
				SoftBody collider = colliders.get(i);
				if (collider.isCreature) {
					float distance = EvolvioColor.dist((float) px, (float) py, (float) collider.px,
							(float) collider.py);
					double combinedRadius = getRadius() * FIGHT_RANGE + collider.getRadius();
					if (distance < combinedRadius) {
						((Creature) collider).dropEnergy(fightLevel * INJURED_ENERGY * timeStep);
					}
				}
			}
		} else {
			fightLevel = 0;
		}
	}

	public void loseEnergy(double energyLost) {
		if (energyLost > 0) {
			energy -= energyLost;
		}
	}

	public void dropEnergy(double energyLost) {
		if (energyLost > 0) {
			if (Double.isNaN(energyLost)){
				System.out.println("Name: " + name);
				System.out.println("Energy: " + energy);
			}
			else{
				energyLost = Math.min(energyLost, energy);
				energy -= energyLost;
				getRandomCoveredTile().addFood(energyLost, hue, true);
			}
		}
	}

	public void see(double timeStep) {
		for (int k = 0; k < visionAngles.length; k++) {
			double visionStartX = px;
			double visionStartY = py;
			double visionTotalAngle = rotation + visionAngles[k];
			

			double dist = visionDistances[k];
			double loseScale = 1000;
			if(visionDistanceIsBrainControlled){
				if (dist > 1){
					loseEnergy(dist/loseScale);
				} 
				else if (dist < -1) {
					loseEnergy(-dist/loseScale);
				}
			}

			
			double ang = visionAngles[k];
			if(visionAngleIsBrainControlled){
				if (ang > 1){
					loseEnergy(ang/loseScale);
				} 
				else if (ang < -1) {
					loseEnergy(-ang/loseScale);
				}
			}


			double endX = getVisionEndX(k);
			double endY = getVisionEndY(k);

			visionOccludedX[k] = endX;
			visionOccludedY[k] = endY;
			int c = getColorAt(endX, endY);
			visionResults[k * 3] = board.evolvioColor.hue(c);
			visionResults[k * 3 + 1] = board.evolvioColor.saturation(c);
			visionResults[k * 3 + 2] = board.evolvioColor.brightness(c);

			int tileX = 0;
			int tileY = 0;
			int prevTileX = -1;
			int prevTileY = -1;
			ArrayList<SoftBody> potentialVisionOccluders = new ArrayList<SoftBody>();
			for (int DAvision = 0; DAvision < visionDistances[k] + 1; DAvision++) {
				tileX = (int) (visionStartX + Math.cos(visionTotalAngle) * DAvision);
				tileY = (int) (visionStartY + Math.sin(visionTotalAngle) * DAvision);
				if (tileX != prevTileX || tileY != prevTileY) {
					addPVOs(tileX, tileY, potentialVisionOccluders);
					if (prevTileX >= 0 && tileX != prevTileX && tileY != prevTileY) {
						addPVOs(prevTileX, tileY, potentialVisionOccluders);
						addPVOs(tileX, prevTileY, potentialVisionOccluders);
					}
				}
				prevTileX = tileX;
				prevTileY = tileY;
			}
			double[][] rotationMatrix = new double[2][2];
			rotationMatrix[1][1] = rotationMatrix[0][0] = Math.cos(-visionTotalAngle);
			rotationMatrix[0][1] = Math.sin(-visionTotalAngle);
			rotationMatrix[1][0] = -rotationMatrix[0][1];
			double visionLineLength = visionDistances[k];
			for (int i = 0; i < potentialVisionOccluders.size(); i++) {
				SoftBody body = potentialVisionOccluders.get(i);
				double x = body.px - px;
				double y = body.py - py;
				double r = body.getRadius();
				double translatedX = rotationMatrix[0][0] * x + rotationMatrix[1][0] * y;
				double translatedY = rotationMatrix[0][1] * x + rotationMatrix[1][1] * y;
				if (Math.abs(translatedY) <= r) {
					if ((translatedX >= 0 && translatedX < visionLineLength && translatedY < visionLineLength)
							|| distance(0, 0, translatedX, translatedY) < r
							|| distance(visionLineLength, 0, translatedX, translatedY) < r) { // YES!
																								// There
																								// is
																								// an
																								// occlussion.
						visionLineLength = translatedX - Math.sqrt(r * r - translatedY * translatedY);
						visionOccludedX[k] = visionStartX + visionLineLength * Math.cos(visionTotalAngle);
						visionOccludedY[k] = visionStartY + visionLineLength * Math.sin(visionTotalAngle);
						visionResults[k * 3] = body.hue;
						visionResults[k * 3 + 1] = body.saturation;
						visionResults[k * 3 + 2] = body.brightness;
					}
				}
			}
		}
	}

	public int getColorAt(double x, double y) {
		if (x >= 0 && x < board.boardWidth && y >= 0 && y < board.boardHeight) {
			return board.tiles[(int) (x)][(int) (y)].getColor();
		} else {
			return board.BACKGROUND_COLOR;
		}
	}

	public double distance(double x1, double y1, double x2, double y2) {
		return (Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)));
	}

	public void addPVOs(int x, int y, ArrayList<SoftBody> PVOs) {
		if (x >= 0 && x < board.boardWidth && y >= 0 && y < board.boardHeight) {
			for (int i = 0; i < board.softBodiesInPositions[x][y].size(); i++) {
				SoftBody newCollider = (SoftBody) board.softBodiesInPositions[x][y].get(i);
				if (!PVOs.contains(newCollider) && newCollider != this) {
					PVOs.add(newCollider);
				}
			}
		}
	}

	public void returnToEarth() {
		int pieces = 20;
		//double radius = (float) getRadius();
		for (int i = 0; i < pieces; i++) {
			getRandomCoveredTile().addFood(energy / pieces, hue, true);
		}
		for (int x = SBIPMinX; x <= SBIPMaxX; x++) {
			for (int y = SBIPMinY; y <= SBIPMaxY; y++) {
				board.softBodiesInPositions[x][y].remove(this);
			}
		}
		if (board.selectedCreature == this) {
			board.unselect();
		}
	}

	public void reproduce(double babySize, double timeStep) {
		// Probability of there being a mutation
		boolean mutateBrain = false;
		if(numChildren >= NUM_CHILDREN_BEFORE_MUTATING){
			mutateBrain = Math.random() <= 0.5;
		}

		if (colliders == null) {
			collide(timeStep);
		}
		int highestGen = 0;
		int highestMut = 0;
		if (babySize >= 0) {
			ArrayList<Creature> parents = new ArrayList<Creature>(0);
			parents.add(this);
			double availableEnergy = getBabyEnergy();
			for (int i = 0; i < colliders.size(); i++) {
				SoftBody possibleParent = colliders.get(i);
				if (possibleParent.isCreature && ((Creature) possibleParent).brain.outputs()[EYE_COUNT*3] > -1) { // Must
																										// be
																										// a
																										// WILLING
																										// creature
																										// to
																										// also
																										// give
																										// birth.
					float distance = EvolvioColor.dist((float) px, (float) py, (float) possibleParent.px,
							(float) possibleParent.py);
					double combinedRadius = getRadius() * FIGHT_RANGE + possibleParent.getRadius();
					if (distance < combinedRadius) {
						parents.add((Creature) possibleParent);
						availableEnergy += ((Creature) possibleParent).getBabyEnergy();
					}
				}
			}
			if (availableEnergy > babySize) {
				double newPX = board.evolvioColor.random(-0.01f, 0.01f);
				double newPY = board.evolvioColor.random(-0.01f, 0.01f);
				while (newPY == 0f){
					newPY = board.evolvioColor.random(-0.01f, 0.01f);
				}
																		// To
																		// avoid
																		// landing
				// directly on
				// parents,
				// resulting in
				// division by 0)
				//newPX = 0f;
				//newPY = 0f;
				double newHue = 0;
				double newSaturation = 0;
				double newBrightness = 0;
				double newMouthHue = 0;
				int parentsTotal = parents.size();
				String[] parentNames = new String[parentsTotal];
				int[] parentIds= new int[parentsTotal];
				Brain newBrain = brain.evolve(parents);
				for (int i = 0; i < parentsTotal; i++) {
					int chosenIndex = (int) board.evolvioColor.random(0, parents.size());
					Creature parent = parents.get(chosenIndex);
					parents.remove(chosenIndex);
					double lostEnergy = babySize * (parent.getBabyEnergy() / availableEnergy);
					if(Double.isNaN(lostEnergy) || Double.isInfinite(lostEnergy)){
						System.out.println("Name: " + parent.name);
						System.out.println("Energy: " + parent.energy);
					}
					else if(!mutateBrain){
						parent.energy -= lostEnergy;
					}
					newPX += parent.px / parentsTotal;
					newPY += parent.py / parentsTotal;
					newHue += parent.hue / parentsTotal;
					newSaturation += parent.saturation / parentsTotal;
					newBrightness += parent.brightness / parentsTotal;
					newMouthHue += parent.mouthHue / parentsTotal;
					parentNames[i] = parent.name;
					parentIds[i] = parent.id;
					
					if (parent.gen > highestGen) {
						highestGen = parent.gen;
					}
					
					if (parent.mut > highestMut) {
						highestMut = parent.mut;
					}
					
					
				}
				newSaturation = 1;
				newBrightness = 1;
				if (mutateBrain){
					newBrain = new Brain(board, null, null);
					highestGen = 0;
					highestMut++;
				}
				numChildren++;
				board.creatures.add(new Creature(newPX, newPY, 0, 0, babySize, density, newHue,
						newSaturation, newBrightness, board,
						board.evolvioColor.random(0, 2 * EvolvioColor.PI), 0, stitchName(parentNames),
						andifyParents(parentNames), true, newBrain, highestGen + 1, newMouthHue, highestMut));
			}
		}
	}

	public String stitchName(String[] s) {
		String result = "";
		for (int i = 0; i < s.length; i++) {
			float portion = ((float) s[i].length()) / s.length;
			int start = Math.min(Math.max(Math.round(portion * i), 0), s[i].length());
			int end = Math.min(Math.max(Math.round(portion * (i + 1)), 0), s[i].length());
			result = result + s[i].substring(start, end);
		}
		return result;
	}

	public String andifyParents(String[] s) {
		String result = "";
		for (int i = 0; i < s.length; i++) {
			if (i >= 1) {
				result = result + " & ";
			}
			result = result + capitalize(s[i]);
		}
		return result;
	}

	public String getCreatureName() {
		if (EvolvioColor.showStats){
			return capitalize(name) + " (" + mut + "," + gen + ")";
		} else {
			return capitalize(name);
		}
		
	}

	public String capitalize(String n) {
		return n.substring(0, 1).toUpperCase() + n.substring(1, n.length());
	}

	@Override
	public void applyMotions(double timeStep) {
		if (getRandomCoveredTile().fertility > 1) {
			loseEnergy(SWIM_ENERGY * energy);
		}
		super.applyMotions(timeStep);
		rotation += vr;
		vr *= Math.max(0, 1 - FRICTION / getMass());
	}

	public double getEnergyUsage(double timeStep) {
		return (energy - previousEnergy[ENERGY_HISTORY_LENGTH - 1]) / ENERGY_HISTORY_LENGTH / timeStep;
	}

	public double getBabyEnergy() {
		return energy - SAFE_SIZE;
	}

	public void addEnergy(double amount) {
		energy += amount;
	}

	public void setPreviousEnergy() {
		for (int i = ENERGY_HISTORY_LENGTH - 1; i >= 1; i--) {
			previousEnergy[i] = previousEnergy[i - 1];
		}
		previousEnergy[0] = energy;
	}

	// What is being measured to rank creatures
	public double measure(int choice) {
		int sign = 1 - 2 * (choice % 2);
		if (choice < 2) {
			return sign * energy;
		} else if (choice < 4) {
			return sign * birthTime;
		} else if (choice == 6 || choice == 7) {
			return sign * gen;
		} else if (choice == 8 || choice == 9) {
			return sign * mut;
		}
		return 0;
	}

	public void setHue(double set) {
		hue = Math.min(Math.max(set, 0), 1);
	}

	public void setMouthHue(double set) {
		mouthHue = Math.min(Math.max(set, 0), 1);
	}

	public void setSaturarion(double set) {
		saturation = Math.min(Math.max(set, 0), 1);
	}

	public void setBrightness(double set) {
		brightness = Math.min(Math.max(set, 0), 1);
	}

	/*
	 * public void setVisionAngle(double set) { visionAngle =
	 * set;//Math.min(Math.max(set, -Math.PI/2), Math.PI/2); while(visionAngle <
	 * -Math.PI) { visionAngle += Math.PI*2; } while(visionAngle > Math.PI) {
	 * visionAngle -= Math.PI*2; } } public void setVisionDistance(double set) {
	 * visionDistance = Math.min(Math.max(set, 0), MAX_VISION_DISTANCE); }
	 */
	/*
	 * public double getVisionStartX() { return
	 * px;//+getRadius()*Math.cos(rotation); } public double getVisionStartY() {
	 * return py;//+getRadius()*Math.sin(rotation); }
	 */

	public double getVisionEndX(int i) {
		double visionTotalAngle = rotation + visionAngles[i];
		return px + visionDistances[i] * Math.cos(visionTotalAngle);
	}

	public double getVisionEndY(int i) {
		double visionTotalAngle = rotation + visionAngles[i];
		return py + visionDistances[i] * Math.sin(visionTotalAngle);
	}
}