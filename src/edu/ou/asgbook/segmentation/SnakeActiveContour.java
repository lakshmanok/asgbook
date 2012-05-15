/**
 * 
 */
package edu.ou.asgbook.segmentation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.ou.asgbook.core.LatLonGrid;
import edu.ou.asgbook.dataset.GlobalPopulation;
import edu.ou.asgbook.filters.DilationFilter;
import edu.ou.asgbook.filters.LoGEdgeFilter;
import edu.ou.asgbook.filters.SimpleThresholder;
import edu.ou.asgbook.io.KmlWriter;
import edu.ou.asgbook.io.OutputDirectory;
import edu.ou.asgbook.io.PngWriter;

/**
 * Active contour method of identifying objects.
 * 
 * @author v.lakshmanan
 *
 */
public class SnakeActiveContour {
	private final static double CONTINUITY_WEIGHT = 0.6;
	private final static double CURVATURE_WEIGHT = 0.1;
	private final static double GRADIENT_WEIGHT = 0.3;
	private final int SNAKE_LENGTH = 20; // points on snake
	private final int SNAKE_DIST_BETWEEN_PTS = 5;
	
	private static final int GRADIENT_SCALE = 100;
	private final LatLonGrid gradient;

	public SnakeActiveContour(LatLonGrid gradient){
		// Normalize the gradient grid to lie between 0 and 100
		this.gradient = LatLonGrid.copyOf(gradient);
		int maxgradient = 0;
		for (int i=0; i < gradient.getNumLat(); ++i) for (int j=0; j < gradient.getNumLon(); ++j){
			if ( gradient.getValue(i, j) > maxgradient ){
				maxgradient = gradient.getValue(i, j);
			}
		}
		for (int i=0; i < gradient.getNumLat(); ++i) for (int j=0; j < gradient.getNumLon(); ++j){
			if ( gradient.getValue(i, j) != gradient.getMissing() ){
				this.gradient.setValue(i, j, (gradient.getValue(i, j) * GRADIENT_SCALE) / maxgradient);
			} else {
				this.gradient.setValue(i, j, 0);
			}
		}	
	}
		
	public static class SnakeNode {
		int x;
		int y;
		double alpha, beta, gamma, curv;
		public SnakeNode(int x, int y){
			this.x = x;
			this.y = y;
			this.alpha = CONTINUITY_WEIGHT;
			this.beta = CURVATURE_WEIGHT;
			this.gamma = GRADIENT_WEIGHT;
			this.curv = 0;
		}
		private static int validate(int x, int maxx){
			if ( x < 0 ) x = 0;
			else if ( x >= maxx ) x = maxx - 1;
			return x;
		}
		
		public SnakeNode(int x, int y, int maxx, int maxy){
			this(validate(x,maxx), validate(y,maxy));
		}
		
		public SnakeNode(double x, double y, int maxx, int maxy){
			this((int)Math.round(x), (int)Math.round(y), maxx, maxy);
		}

		public int getDistanceSquared(SnakeNode other){
			int distx = x - other.x;
			int disty = y - other.y;
			return distx*distx + disty*disty;
		}
		public int getDistanceSquared(int otherx, int othery){
			int distx = x - otherx;
			int disty = y - othery;
			return distx*distx + disty*disty;
		}
		int getX(){
			return x;
		}
		int getY(){
			return y;
		}
	}
	
	public class Snake {
		private SnakeNode[] pts;
		private double meanDistBetweenPts;

		public Snake(SnakeNode[] pts){
			this.pts = pts;
			meanDistBetweenPts = 0;
			if ( this.pts.length == 0 ) return;
			
			// compute mean dist
			for (int i=0; i < pts.length; ++i){
				SnakeNode curr = pts[i];
				SnakeNode next = pts[ (i+1)%(pts.length) ];
				meanDistBetweenPts += Math.sqrt( curr.getDistanceSquared(next) );
			}
			meanDistBetweenPts /= pts.length;
		}

		/** the snake is a closed curve, so does modulo to get points **/
		public SnakeNode get(int k){
			int len = pts.length;
			while ( k < 0 ){
				k += len;
			}
			return pts[k%len];
		}
		
		public SnakeNode[] getNodes(){
			return pts;
		}
		
		public double computeEnergy(int candx, int candy, SnakeNode current, SnakeNode previous, SnakeNode next){
			double E_total, E_edgestrength, E_smoothness, E_continuity ; 
			E_edgestrength = gradient.getData()[candx][candy];
			E_smoothness = Math.pow(previous.getX() - 2 * candx + next.getX(), 2) + Math.pow(previous.getY() - 2 * candy + next.getY(), 2);
			E_continuity = Math.abs( Math.sqrt(previous.getDistanceSquared(candx,candy)) - SNAKE_DIST_BETWEEN_PTS );
			E_total =  current.alpha * E_continuity + current.beta * E_smoothness - current.gamma * E_edgestrength;
			return E_total;
		}
	}

	public Snake pruneAndResample(Snake inputSnake){
		List<SnakeNode> nodes = new ArrayList<SnakeNode>(Arrays.asList(inputSnake.pts));
		int numNodes = nodes.size();
		if (numNodes <= 3) return inputSnake;

		
		int dist_thresh = SNAKE_DIST_BETWEEN_PTS * SNAKE_DIST_BETWEEN_PTS;
		for (int i = 0; i < numNodes; i++) {
			SnakeNode curPt = nodes.get(i);
			int next = i + 1; if (next == numNodes) next = 0;
			SnakeNode nextPt = nodes.get(next);

			int distsq = (nextPt.getX()-curPt.getX())*(nextPt.getX()-curPt.getX())+(nextPt.getX()-curPt.getX())*(nextPt.getY()-curPt.getY());
			boolean currNotOnGradient = gradient.getData()[curPt.getX()][curPt.getY()] < 30;
			boolean nextNotOnGradient = gradient.getData()[nextPt.getX()][nextPt.getY()] < 30;
			
			boolean remove = numNodes > SNAKE_LENGTH && ( (distsq < 20) || (distsq < 80 && nextNotOnGradient));
			if (remove) {
				nodes.remove(next);
				--numNodes;
				--i; // retry this node
			} else if (distsq > dist_thresh && (currNotOnGradient || nextNotOnGradient)) {
				SnakeNode newPt = new SnakeNode((curPt.getX()+nextPt.getX())/2,(curPt.getY()+nextPt.getY())/2);
				nodes.add(i+1, newPt);
				numNodes++;
			}
		}
		return new Snake(nodes.toArray(new SnakeNode[0]));
	}
	
	public Snake resampleNodes(Snake inputSnake){
		SnakeNode[] full = complete(inputSnake.pts);
		if ( full.length <= SNAKE_LENGTH ) return inputSnake;
		System.out.println("Resampling " + full.length + " to " + SNAKE_LENGTH);
		int sampleInterval = full.length / SNAKE_LENGTH;
		SnakeNode[] sampled = new SnakeNode[SNAKE_LENGTH];
		for (int i=0; i < sampled.length; ++i){
			sampled[i] = full[i*sampleInterval];
		}
		return new Snake(sampled);
	}
	
	public Snake resample(Snake inputSnake){
		return pruneAndResample(inputSnake);
	}
	
	/**
	 * Provide an initial guess of points
	 */
	public SnakeNode[] moveSnake(SnakeNode[] pixels, int numIter){
		Snake snake = new Snake(pixels);
		snake = resample(snake); // get it to desired length, then start moving it
		snake = moveSnake(snake, numIter);
		return complete(snake.pts);
	}
	
	private SnakeNode[] complete(SnakeNode[] inputs){
		List<SnakeNode> out = new ArrayList<SnakeNode>();
		for (int i=0; i < inputs.length; ++i){
			SnakeNode curr = inputs[i];
			int nexti = i+1; if (nexti == inputs.length) nexti = 0;
			SnakeNode next = inputs[nexti];
			int distx = Math.abs(curr.getX() - next.getX());
			int disty = Math.abs(curr.getY() - next.getY());
			List<SnakeNode> line = new ArrayList<SnakeNode>();
			boolean reverse = false;
			if ( distx > disty ){
				// draw line in x
				SnakeNode start = curr;
				SnakeNode end = next;
				if ( start.getX() > end.getX() ){
					start = next;
					end = curr;
					reverse = true;
				}
				for (int x = start.getX(); x <= end.getX(); ++x){
					double frac = (double) (x - start.getX()) / distx;
					int y = (int) Math.round(start.getY() + frac * (end.getY() - start.getY()));
					SnakeNode p = new SnakeNode(x,y);
					line.add(p);
				}
			} else {
				// draw line in y
				SnakeNode start = curr;
				SnakeNode end = next;
				if ( start.getY() > end.getY() ){
					start = next;
					end = curr;
					reverse = true;
				}
				for (int y = start.getY(); y <= end.getY(); ++y){
					double frac = (double) (y - start.getY()) / disty;
					int x = (int) Math.round(start.getX() + frac * (end.getX() - start.getX()));
					SnakeNode p = new SnakeNode(x,y);
					line.add(p);
				}
			}
			// add points on line to output
			if (reverse){
				Collections.reverse(line);
			}
			out.addAll(line);
		}
		if (out.size() < inputs.length){
			System.out.println("oops ...");
			System.exit(-1);
		}
		return out.toArray(new SnakeNode[0]);
	}
	
	private Snake moveSnake(Snake inputSnake, int numIter){
		Snake snake = inputSnake;
		int numMoved = 0;
		int len = snake.pts.length;
		for (int i = 0; i < len; i++) {
			SnakeNode current = snake.get(i);
			SnakeNode previous = snake.get(i-1);
			SnakeNode next = snake.get(i+1);
			
			// find energy at current point
			double min_energy = snake.computeEnergy(current.getX(), current.getY(),current,previous,next);
			SnakeNode best = current;
			
			// find minimum energy at neighboring points
			for (int m=-1; m <=1; ++m){
				for (int n=-1; n <=1; ++n){
					int candx = current.getX() + m;
					int candy = current.getY() + n;
					if ( gradient.isValid(candx, candy)){
						double energy = snake.computeEnergy(candx,candy,current,previous,next);
						if ( energy < min_energy ){
							best = new SnakeNode(candx, candy);
							min_energy = energy;
						}
					}
				}
			}

			// Move this point on the snake if needed
			if ( best != current ){
				// recompute snake
				SnakeNode[] pixels = snake.getNodes(); // have to get underlying array
				pixels[i%len] = best;
				snake = new Snake(pixels);
				++numMoved;
			}
		}
		System.out.println("iter#" + numIter + ": " + numMoved + " points on snake were moved ... ");
		
		// Resample the snake
		computeCurvature(snake);
		snake = resample(snake);
		
		if (numIter > 0 && numMoved > 0){
			return moveSnake(snake, numIter-1);
		} else {
			return snake;
		}
	}

	private void computeCurvature(Snake snake) {
		// compute curvature
		int len = snake.getNodes().length;
		for (int i = 0; i < len; i++) {
			SnakeNode current = snake.get(i);
			SnakeNode next = snake.get(i+1);
			current.curv = Math.pow(2 * Math.sin(
							Math.toRadians(getAngleBtwVectors(
							current.x, current.y, next.x, next.y)/2)), 2);

		}

		// Find corners by ensuring thresholds met.
		for (int i = 0; i < len; i++) {
			SnakeNode current = snake.get(i);
			SnakeNode previous = snake.get(i-1);
			SnakeNode next = snake.get(i+1);

			if (current.curv > previous.curv && 
					current.curv > next.curv &&
					current.curv > 0.002 &&
					current.beta != 0 &&
					gradient.getValue(current.x,current.y) > 40) {
				current.beta = 0;
			}
		}
	}
	
	private double getAngleBtwVectors(int x1, int y1, int x2, int y2) {
		double a1, a2;
		a1 = getRotateAngle(x1, y1);
		a2 = getRotateAngle(x2, y2);
		if (Math.abs(a2-a1) > 180)
			return 360-Math.abs(a2-a1);
		else
			return Math.abs(a2-a1);
	}

	private double getRotateAngle(int x, int y) {
			double angle;
			if(x == 0 && y == 0)
				return 0;
			angle = (double)Math.acos(x/Math.sqrt(x*x+y*y));
			if (y < 0)
				angle=2*Math.PI-angle;
			return angle*180/Math.PI;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File out = OutputDirectory.getDefault("snake");
		
		// data
		LatLonGrid grid = GlobalPopulation.read(GlobalPopulation.NORTHAMERICA, new GlobalPopulation.LinearScaling()).crop(1000, 2100, 100, 200);
		KmlWriter.write(grid, out, "orig", PngWriter.createCoolToWarmColormap());
		
		// find cities > 10 px and more than 20,000 residents/km^2
		int thresh = 20;
		Segmenter seg = new HysteresisSegmenter(thresh, thresh-5);
		LabelResult labelResult = seg.label(grid);
		KmlWriter.write(labelResult.label, out, "allcities_"+thresh, PngWriter.createRandomColormap());
		int sizethresh = 10;
		LabelResult pruned = RegionProperty.pruneBySize(labelResult, grid, sizethresh);
		RegionProperty[] prop = RegionProperty.compute(pruned, grid);
		KmlWriter.write(pruned.label, out, "largecities_"+sizethresh, PngWriter.createRandomColormap());
		
		// threshold image and compute gradient of thresholded image
		LatLonGrid binaryImage = new SimpleThresholder(1).threshold(pruned.label);
		binaryImage = new DilationFilter(1).filter(binaryImage);
		KmlWriter.write(binaryImage, out, "thresh", PngWriter.createCoolToWarmColormap());
		LatLonGrid gradient = new LoGEdgeFilter(2,1).edgeFilter(binaryImage);
		KmlWriter.write(gradient, out, "gradient", PngWriter.createCoolToWarmColormap());
				
		// for each city, initialize a snake
		SnakeActiveContour alg = new SnakeActiveContour(gradient);
		int numiter = 30;
		for (int i=1; i < prop.length; ++i){
			double cx = prop[i].getCx();
			double cy = prop[i].getCy();
			// square box enclosing the center point that is larger than core area
			double initsize = 3 * Math.sqrt( prop[i].getSize() );
			SnakeNode[] snakepts = new SnakeNode[4];
			snakepts[0] = new SnakeNode(cx + initsize, cy - initsize, grid.getNumLat(), grid.getNumLon() );
			snakepts[1] = new SnakeNode(cx + initsize, cy + initsize, grid.getNumLat(), grid.getNumLon() );
			snakepts[2] = new SnakeNode(cx - initsize, cy + initsize, grid.getNumLat(), grid.getNumLon() );
			snakepts[3] = new SnakeNode(cx - initsize, cy - initsize, grid.getNumLat(), grid.getNumLon() );
	
			SnakeNode[] snake = alg.moveSnake(snakepts, numiter);
			// mark snake points on grid
			for (int k=0; k < snake.length; ++k){
				grid.setValue(snake[k].getX(), snake[k].getY(), 1000);
			}
		}
		
		// write out original grid with snake points marked
		KmlWriter.write(grid, out, "snakes", PngWriter.createCoolToWarmColormap());
	}

}
