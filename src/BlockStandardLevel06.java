
import java.time.LocalDateTime;
import java.util.HashMap;

import org.newdawn.slick.opengl.Texture;
/**
 * Template level for simplify level-building.
 * Copy the code here into a new BlockStandardLevel extended class and edit
 * where necessary to set level difficulty. 
 * @author John
 */
public class BlockStandardLevel06 extends BlockStandardLevel {
	public BlockStandardLevel06(HashMap<String,Texture> rootTexMap) {
		level = 2;
		// TODO: [CUSTOM] set background and user interface sprites
		// if these sprite must be defined or the game will crash at runtime
		background = new Sprite(
				rootTexMap.get("bg_space_1"),
				new int[] { 0, 0 },
				new int[] { 1024, 768 },
				new int[] { Global.glEnvWidth, Global.glEnvHeight }
			);
		
		userInterface = new Sprite(
				rootTexMap.get("ui_stdmode"), // default interface texture
				new int[] { 0, 0 },
				new int[] { 1024, 768 },
				new int[] { Global.glEnvWidth, Global.glEnvHeight }
			);
		// TODO: [CUSTOM] set the score multiplier for the level
		// default value: 1.0f
		levelMultiplier = 1.5f;
		
		// TODO: [CUSTOM] set the energy gain multiplier. default value is 1.0f
		energyGainMultiplier = 1.0f;
		
		// TODO: [CUSTOM] set the block size and grid size
		// valid block dimensions are { 16, 32, 64 }
		// valid grid sizes (respective to block size) are { 40, 20, 10 }
		blockSize = new int[] { 32, 32 }; // default block size is { 32, 32 }
		gridSize = new int[] { 20, 20 }; // default grid size is { 20, 20 }
		// create the grid with x-dimension as specified above
		grid = new GridColumn[gridSize[0]];
		queue = new Block[gridSize[0]];
		// build the grid according the level difficulty
		buildGrid();
		// set the grid draw starting position derived from grid and block size
		gridBasePos = new int[] { 20, Global.glEnvHeight - blockSize[1] - 50 };
		// set the cursor starting position in the center of the grid
		cursorGridPos[0] = grid.length / 2;
		cursorGridPos[1] = grid[0].blocks.length / 2;
		// set energy max if not default
		energy = energyMax = 200000;
		
		setGridCounts();
	}
	
	@Override
	protected void buildGrid() {
		Block b = null;
		Global.rand.setSeed(LocalDateTime.now().getNano());
		for (int i = 0; i < grid.length; i++) {
			grid[i] = new GridColumn(gridSize[1]);
			for (int k = 0; k < grid[0].blocks.length; k++) {
				// TODO: [CUSTOM] define the randomly generated blocks rate of appearance 
				b = new Block(Block.BlockType.BLOCK, Global.rand.nextInt(3));
				grid[i].blocks[k] = b;
			}
		}
		grid[Global.rand.nextInt(grid.length)].blocks[Global.rand.nextInt(grid[0].blocks.length)] = new Block(Block.BlockType.HEART);

		// set the block count for the level
		blocksRemaining = grid.length * grid[0].blocks.length;
		// TODO: [CUSTOM] add any custom/special blocks that have limited generation (rocks, trash, wedge, etc.)
		// remember to decrease blocksRemaining for each such block added 
		int rx, ry;
		rx = Global.rand.nextInt(10) + 5;
		ry = Global.rand.nextInt(4) + 8;
		grid[rx].blocks[ry] = new Block(Block.BlockType.WEDGE);
		blocksRemaining--;
		wedgePos = new int[] { rx, ry };
	}

	@Override
	protected Block getQueueBlock() {
		Block b = null;
		// TODO: [CUSTOM] define the type and rate of blocks that are added to the grid via the queue
		b = new Block(Block.BlockType.BLOCK, Global.rand.nextInt(3));

		return b;		
	}
}
