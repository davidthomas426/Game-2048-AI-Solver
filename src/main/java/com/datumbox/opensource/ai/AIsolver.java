/* 
 * Copyright (C) 2014 Vasilis Vryniotis <bbriniotis at datumbox.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.datumbox.opensource.ai;

import com.datumbox.opensource.dataobjects.Direction;
import com.datumbox.opensource.game.Board;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The AIsolver class that uses Artificial Intelligence to estimate the next move.
 * 
 * @author Vasilis Vryniotis <bbriniotis at datumbox.com>
 */
public class AIsolver {
    
    /**
     * Player vs Computer enum class
     */
    public enum Player {
        /**
         * Computer
         */
        COMPUTER, 

        /**
         * User
         */
        USER
    }
    
    /**
     * Method that finds the best next move.
     * 
     * @param theBoard
     * @param depth
     * @return
     * @throws CloneNotSupportedException 
     */
    public static Direction findBestMove(Board theBoard, int depth) throws CloneNotSupportedException {
        //Map<String, Object> result = minimax(theBoard, depth, Player.USER);
        
        Map<String, Object> result = alphabeta(theBoard, depth, Player.USER);
        
        return (Direction)result.get("Direction");
    }
    
    /**
     * Finds the best move by using the Minimax algorithm.
     * 
     * @param theBoard
     * @param depth
     * @param player
     * @return
     * @throws CloneNotSupportedException 
     */
    private static Map<String, Object> minimax(Board theBoard, int depth, Player player) throws CloneNotSupportedException {
        Map<String, Object> result = new HashMap<>();
        
        Direction bestDirection = null;
        int bestScore;
        
        if(depth==0 || theBoard.isGameTerminated()) {
            bestScore=heuristicScore(theBoard.getScore(),theBoard.getNumberOfEmptyCells(),calculateClusteringScore(theBoard.getBoardArray()),calculateCornerScoreWrapper(theBoard));
        }
        else {
            if(player == Player.USER) {
                bestScore = Integer.MIN_VALUE;

                for(Direction direction : Direction.values()) {
                    Board newBoard = (Board) theBoard.clone();

                    int points=newBoard.move(direction);
                    
                    if(points==0 && newBoard.isEqual(theBoard.getBoardArray(), newBoard.getBoardArray())) {
                    	continue;
                    }

                    Map<String, Object> currentResult = minimax(newBoard, depth-1, Player.COMPUTER);
                    int currentScore=((Number)currentResult.get("Score")).intValue();
                    if(currentScore>bestScore) { //maximize score
                        bestScore=currentScore;
                        bestDirection=direction;
                    }
                }
            }
            else {
                bestScore = Integer.MAX_VALUE;

                List<Integer> moves = theBoard.getEmptyCellIds();
                if(moves.isEmpty()) {
                    bestScore=0;
                }
                int[] possibleValues = {2, 4};

                int i,j;
                int[][] boardArray;
                for(Integer cellId : moves) {
                    i = cellId/Board.BOARD_SIZE;
                    j = cellId%Board.BOARD_SIZE;

                    for(int value : possibleValues) {
                        Board newBoard = (Board) theBoard.clone();
                        newBoard.setEmptyCell(i, j, value);

                        Map<String, Object> currentResult = minimax(newBoard, depth-1, Player.USER);
                        int currentScore=((Number)currentResult.get("Score")).intValue();
                        if(currentScore<bestScore) { //minimize best score
                            bestScore=currentScore;
                        }
                    }
                }
            }
        }
        
        result.put("Score", bestScore);
        result.put("Direction", bestDirection);
        
        return result;
    }
    
    /**
     * Finds the best move bay using the Alpha-Beta pruning algorithm.
     * 
     * @param theBoard
     * @param depth
     * @param alpha
     * @param beta
     * @param player
     * @return
     * @throws CloneNotSupportedException 
     */
    private static Map<String, Object> alphabeta(Board theBoard, int depth, Player player) throws CloneNotSupportedException {
        Map<String, Object> result = new HashMap<>();
        
        Direction bestDirection = null;
        int bestScore;
        
        if(theBoard.isGameTerminated()) {
            if(theBoard.hasWon()) {
                bestScore=4096*100; //highest possible score - using MAX_VALUE would cause overflow problems
            }
            else {
                bestScore=0;
            }
        }
        else if(depth==0) {
            bestScore=heuristicScore(theBoard.getScore(),theBoard.getNumberOfEmptyCells(),calculateClusteringScore(theBoard.getBoardArray()),calculateCornerScoreWrapper(theBoard));
        }
        else {
        	bestScore = 0;
            if(player == Player.USER) {
                for(Direction direction : Direction.values()) {
                    Board newBoard = (Board) theBoard.clone();

                    int points=newBoard.move(direction);
                    
                    if(points==0 && newBoard.isEqual(theBoard.getBoardArray(), newBoard.getBoardArray())) {
                    	continue;
                    }
                    
                    Map<String, Object> currentResult = alphabeta(newBoard, depth-1, Player.COMPUTER);
                    int currentScore=((Number)currentResult.get("Score")).intValue();
                                        
                    if(currentScore>=bestScore) { //maximize score
                        bestScore=currentScore;
                        bestDirection=direction;
                    }
                }
            }
            else {
                List<Integer> moves = theBoard.getEmptyCellIds();
                int[] possibleValues = {2, 4};
                /*Only consider maxBranches randon new cells.  Start with 2's, top to bottom, left to right.
                 * Note that this would cause problems if we allowed the AI to use any corner, and not just top left.
                */
                int maxBranches = 16; 
                int scoreSum=0;
                
                int branchCnt=0;
                for(int value : possibleValues) {
                    
                	for(Integer cellId : moves) {
                                       	
                        int i = cellId/Board.BOARD_SIZE;
                        int j = cellId%Board.BOARD_SIZE;
                        
                        Board newBoard = (Board) theBoard.clone();
                        newBoard.setEmptyCell(i, j, value);

                        Map<String, Object> currentResult = alphabeta(newBoard, depth-1, Player.USER);
                        int currentScore=((Number)currentResult.get("Score")).intValue();
                        if(value == 2){
                        	scoreSum += 9*currentScore;
                        } else {
                        	scoreSum += currentScore;
                        }
                        branchCnt++;
                        if(branchCnt >= maxBranches) break;
                    }
                	if(branchCnt >= maxBranches) break;
                }
                bestScore = scoreSum / (theBoard.getNumberOfEmptyCells() * 10);
            }
        }
        
        result.put("Score", bestScore);
        result.put("Direction", bestDirection);
        
        return result;
    }
    
    /**
     * Estimates a heuristic score by taking into account the real score, the
     * number of empty cells and the clustering score of the board.
     * 
     * @param actualScore
     * @param numberOfEmptyCells
     * @param clusteringScore
     * @return 
     */
    private static int heuristicScore(int actualScore, int numberOfEmptyCells, int clusteringScore, int cornerScore) {
        int score = (int) (actualScore-300/(numberOfEmptyCells+1) - clusteringScore + 4*cornerScore);
        return Math.max(score, Math.min(actualScore, 1));
    }
    
    private static int calculateCornerScoreWrapper(Board board){
    	return calculateCornerScore(board.getBoardArray());
    	
    	/* Letting the AI solve using any corner is definitely a better idea - I just couldn't get it to work right.
    	 * My guess is that the error is not a simple bug, but complex implications of the corner scoring.
    	 *   
    	 * Consider the following board:
    	 * 4  1024  512  256
    	 * 2     0    0    0
    	 * 0     0    0    0
    	 * 0     0    0    0
    	 * 
    	 * The correct strategy is (probably) to accept the "lost space" in the upper left corner and go for a second row of
    	 * 16   32   64  128
    	 * 
    	 * But the solve any corner metric encourages a right column of 256, 128, 64, 32 etc., which is probably even worse
    	 * than no corner heuristic at all.
    	 * 
    	 * It's probably more important to first add code for "it's okay if you lose a corner - go ahead and try to win
    	 * with a missing square" before allowing the target corner to change.
    	 *  
    	*/
    	
    	/*int score = 0;
    	for(int j=0; j<2; j++){
    		for(int i=0; i<4; i++){
    			score = Math.max(score, calculateCornerScore(board.getBoardArray()));
    			board.rotateLeft();
    		}
    	
    		board.flip();
    	}
    	
    	return score;*/
    }
    
    private static int calculateCornerScore(int[][] boardArray){
    	//return 0;
    	int cornerScore = 0;
    	int limit = 2048*8;
    	for(int i=0; i<4; i++){
    		if(boardArray[0][i] <= limit){
    			cornerScore += boardArray[0][i];
    			limit = boardArray[0][i];
    		} else {
    			return cornerScore;
    		}
    	}
    	
    	for(int i=3; i>=0; i--){
    		if(boardArray[1][i] <= limit){
    			cornerScore += boardArray[1][i];
    			limit = boardArray[1][i];
    		} else {
    			return cornerScore;
    		}
    	}
    	
    	for(int i=0; i<4; i++){
    		if(boardArray[2][i] <= limit){
    			cornerScore += boardArray[2][i];
    			limit = boardArray[2][i];
    		} else {
    			return cornerScore;
    		}
    	}
    	
    	return cornerScore;
    }
    
    /**
     * Calculates a heuristic variance-like score that measures how clustered the
     * board is.
     * 
     * @param boardArray
     * @return 
     */
    private static int calculateClusteringScore(int[][] boardArray) {
        int clusteringScore=0;
        
        int[] neighbors = {-1,0,1};
        
        for(int i=0;i<boardArray.length;++i) {
            for(int j=0;j<boardArray.length;++j) {
                if(boardArray[i][j]==0) {
                    continue; //ignore empty cells
                }
                
                //clusteringScore-=boardArray[i][j];
                
                //for every pixel find the distance from each neightbors
                int numOfNeighbors=0;
                int sum=0;
                for(int k : neighbors) {
                    int x=i+k;
                    if(x<0 || x>=boardArray.length) {
                        continue;
                    }
                    for(int l : neighbors) {
                        int y = j+l;
                        if(y<0 || y>=boardArray.length) {
                            continue;
                        }
                        
                        if(boardArray[x][y]>0) {
                            ++numOfNeighbors;
                            sum+=Math.abs(boardArray[i][j]-boardArray[x][y]);
                        }
                        
                    }
                }
                
                clusteringScore+=sum/numOfNeighbors;
            }
        }
        
        return clusteringScore;
    }

}
