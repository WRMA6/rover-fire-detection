import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import Jama.Matrix;

public class Detector {

    //Static inner class
    static class detectFire {

        public static int detect(BufferedImage input) {

            //For YCbCr conversion
            double[][] cFactorVals = {
                {
                    65.481,
                    128.553,
                    24.966
                },
                {
                	-37.797,
                    -74.203,
                    112.0
                },
                {
                    112.0,
                    -93.786,
                    -18.214
                }
            };
            
            Matrix cFactor = new Matrix(cFactorVals);
            double[][] offsetVals = {
                {16}, {128}, {128}
            };
            
            Matrix offset = new Matrix(offsetVals);
            //find inverse of cFactor
            Matrix cFactorInv = cFactor.inverse();

            //input data
            int width = input.getWidth();
            int height = input.getHeight();
            long numPixels = width * height;
            int counter = 0;

            //Setup for writing YCbCr images
            String Crpath = System.getProperty("user.dir") + "/output/CrFilter.jpg";
            String edgepath = System.getProperty("user.dir") + "/output/EdgeMap.jpg";
            BufferedImage Crcomp = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            BufferedImage edgemap = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            //Variables to hold certain averages (ie. colours) within the image
            int blueTotalY = 0;
            int redTotalY = 0;
            int yeTotalY = 0;
            int blueCounted = 0;
            int redCounted = 0;
            int yeCounted = 0;
            double blueAvgY = 0;
            double redAvgY = 0;
            double yeAvgY = 0;

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    //Get rgb values
                    int r = (getPixel(input, x, y) >> 16) & 0xFF;
                    int g = (getPixel(input, x, y) >> 8) & 0xFF;
                    int b = (getPixel(input, x, y)) & 0xFF;
                    b = rgbBounded(b - 50);
                    
                    double[][] rgbVals = {
                        {r}, {g}, {b}
                    };
                    Matrix rgb = new Matrix(rgbVals);

                    //Set up YCbCr
                    double Y = 0;
                    double Cb = 0;
                    double Cr = 0;
                    double[][] YCbCrVals = {
                        {Y}, {Cb}, {Cr}
                    };
                    Matrix YCbCr = new Matrix(YCbCrVals);

                    YCbCr = offset.plus(cFactor.times(rgb));
                    Y = YCbCr.get(0, 0);
                    Cb = YCbCr.get(1, 0);
                    Cr = YCbCr.get(2, 0);

                    //Y comp
                    int rY = 0;
                    int gY = 0;
                    int bY = 0;
                    
                    double[][] rgbYVals = {
                        {rY}, {gY}, {bY}
                    };
                    Matrix rgbY = new Matrix(rgbYVals);
                    
                    double[][] set1Vals = {
                        {Y}, {0}, {0}
                    };
                    Matrix set1 = new Matrix(set1Vals);

                    rgbY = cFactorInv.times(set1.minus(offset));
                    rY = (int) rgbY.get(0, 0);
                    gY = (int) rgbY.get(1, 0);
                    bY = (int) rgbY.get(2, 0);

                    //Cb comp
                    int rCb = 0;
                    int gCb = 0;
                    int bCb = 0;
                    
                    double[][] rgbCbVals = {
                        {rCb}, {gCb}, {bCb}
                    };
                    Matrix rgbCb = new Matrix(rgbCbVals);
                    
                    double[][] set2Vals = {
                        {0}, {Cb}, {0}
                    };
                    Matrix set2 = new Matrix(set2Vals);

                    rgbCb = cFactorInv.times(set2.minus(offset));
                    rCb = (int) rgbCb.get(0, 0);
                    gCb = (int) rgbCb.get(1, 0);
                    bCb = (int) rgbCb.get(2, 0);

                    //Cr comp
                    int rCr = 0;
                    int gCr = 0;
                    int bCr = 0;
                    
                    double[][] rgbCrVals = {
                        {rCr}, {gCr}, {bCr}
                    };
                    Matrix rgbCr = new Matrix(rgbCrVals);
                    
                    double[][] set3Vals = {
                        {0}, {0}, {Cr}
                    };
                    Matrix set3 = new Matrix(set3Vals);

                    rgbCr = cFactorInv.times(set3.minus(offset));
                    rCr = (int) rgbCr.get(0, 0);
                    gCr = (int) rgbCr.get(1, 0);
                    bCr = (int) rgbCr.get(2, 0);

                    //Make sure rgb values are bounded by 0 and 255
                    //rY = rgbBounded(rY);
                    //gY = rgbBounded(gY);
                    //bY = rgbBounded(bY);
                    //rCb = rgbBounded(rCb);
                    //gCb = rgbBounded(gCb);
                    //bCb = rgbBounded(bCb);
                    rCr = rgbBounded(rCr);
                    gCr = rgbBounded(gCr);
                    bCr = rgbBounded(bCr);

                    //Make the components monochrome
                    //int avg1 = (rCb + gCb + bCb)/3;
                    int avg2 = (rCr + gCr + bCr) / 3;

                    //Dynamically increase levels in areas that are 'expected' to have fire, 
                    //Based on somewhat experimental thresholds
                    final int t1 = 15;
                    final int t2 = 100; //at-least red threshold
                    final int t3 = 210; //at most green threshold
                    final int t4 = 200; //at most blue threshold
                    final int boostVal = 150;

                    //if(avg1 < t1) {avg1 = 0;}
                    if (avg2 < t1) {
                        avg2 = 0;
                    }
                    
                    if (avg2 >= t1 && r > t2 && g < t3 && b < t4 && (r > g && g > b)) {
                        avg2 += boostVal;
                        counter++;
                    }

                    //avg1 = rgbBounded(avg1);
                    avg2 = rgbBounded(avg2);

                    //rCb = avg1; gCb = avg1; bCb = avg1;
                    rCr = avg2;
                    gCr = avg2;
                    bCr = avg2;

                    if ((b > g && b > r) && ((b >= 230 || (b >= 2 * r)))) { 
                    	//(b > g and b > r) && (b >= 230) || (b >= 2r) considered blue
                        blueTotalY += y;
                        blueCounted++;
                    }
                    
                    if (avg2 >= boostVal) { 
                    	//(r > g && r > b) considered red
                        redTotalY += y;
                        redCounted++;
                    }
                    
                    if (r > 235 && g > 235) { 
                    	//both red and green higher than 235 considered yellow
                        yeTotalY += y;
                        yeCounted++;
                    }

                    //Edit bufferedImage objects
                    int a = 255;
                    //int pY = 0;
                    //int pCb = 0;
                    int pCr = 0;

                    //pY = (a<<24) | (rY<<16) | (gY<<8) | bY;
                    //pCb = (a<<24) | (rCb<<16) | (gCb<<8) | bCb;
                    pCr = (a << 24) | (rCr << 16) | (gCr << 8) | bCr;

                    //pY = ((int)Y<<16) | ((int)0<<8) | (int)0;
                    //pCb = ((int)0<<16) | ((int)Cb<<8) | (int)0;
                    //pCr = ((int)0<<16) | ((int)0<<8) | (int)Cr;

                    //Ycomp.setRGB(x, y, pY);
                    //Cbcomp.setRGB(x, y, pCb);
                    Crcomp.setRGB(x, y, pCr);

                    //alt way
                    /*
                    Ycompg.setColor(new Color(pY));
                    Cbcompg.setColor(new Color(pCb));
                    Crcompg.setColor(new Color(pCr));
					
                    Ycompg.fillRect(x, y, 1, 1);
                    Cbcompg.fillRect(x, y, 1, 1);
                    Crcompg.fillRect(x, y, 1, 1);
                    */

                    //initialize edgemap image to be all black
                    int pEM = (255 << 24) | (0 << 16) | (0 << 8) | 0;
                    edgemap.setRGB(x, y, pEM);

                }

            }

            //Edgemap part
            int[][] vertFilter = {
                {
                    -1, 0, 1
                },
                {
                	-2, 0, 2
                },
                {
                	-1, 0, -1
                }
            };
            
            int[][] horFilter = {
                {
                    1, 2, 1
                },
                {
                    0, 0, 0
                },
                {
                	-1, -2, -1
                }
            };

            for (int x = 1; x < width - 1; x++) {

                for (int y = 1; y < height - 1; y++) {

                    //3x3 array of colours about center
                    int[][] gray = new int[3][3];
                    
                    for (int i = 0; i < 3; i++) {

                        for (int j = 0; j < 3; j++) {

                            gray[i][j] = (int) 
                            		getIntensity((getPixel(input, x - 1 + i, y - 1 + j)));
                            //gray[i][j] = getPixel(Crcomp, x-1+i, y-1+j);
                        }
                    }

                    int grayV = 0;
                    int grayH = 0;
                    
                    for (int i = 0; i < 3; i++) {

                        for (int j = 0; j < 3; j++) {

                            grayV += gray[i][j] * vertFilter[i][j];
                            grayH += gray[i][j] * horFilter[i][j];
                        }
                    }
                    
                    int avg = 255 - rgbBounded((int) Math.sqrt(grayV * grayV + grayH * grayH));
                    int newP = (255 << 24) | (avg << 16) | (avg << 8) | avg;
                    if (avg > 5) {
                        counter++;
                    }
                    edgemap.setRGB(x, y, newP);
                }
            }

            //Attempt to make conclusion
            int result = 0;

            //Most lit pixels in Cr should be black on edgemap
            int trueCounter = 0;
            int falseCounter = 0;
            int total = 0;
            double passRate = 0;
            for (int x = 0; x < width; x++) {

                for (int y = 0; y < height; y++) {

                    if ((Crcomp.getRGB(x, y) >> 16 & 0xFF) >= 100) {
                        total++;
                        if ((edgemap.getRGB(x, y) >> 16 & 0xFF) <= 50) {
                            trueCounter++;
                        } else {
                            falseCounter++;
                        }
                    }
                }
            }
            
            passRate = (double) trueCounter / (double) total;
            System.out.println("Pass rate: " + passRate);

            //Ratio of pixels in edgemap
            double ratio = 0;
            ratio = (double) counter / ((double)(numPixels * 2.0));
            System.out.println("Old ratio: " + ratio);

            //Average positions of different colours
            blueAvgY = (double) blueTotalY / (double) blueCounted;
            redAvgY = (double) redTotalY / (double) redCounted;
            yeAvgY = (double) yeTotalY / (double) yeCounted;

            System.out.println("Bavg: " + blueAvgY + " Ravg: " + redAvgY + " Yavg: " + yeAvgY);

            //If blue avg farther down image than red + x percent of height for variance
            if ((blueCounted > 50 && redCounted > 50)
            		&& (blueAvgY > (redAvgY + 0.1 * (double) height))) {

                System.out.println("Result modified due to average blue below red");
                //result = 0;
                ratio -= 0.40;
            }

            //Yellow and red avg in proper relative locations (ie. about the same)
            if ((redCounted > 50 && yeCounted > 50) && (Math.abs(redAvgY - yeAvgY) <= 10)) {
                System.out.println("Result modified due to average yellow and red nearby");
                ratio += 0.15;
            }

            if (redCounted < 100) {
                ratio -= 0.3;
            }

            if (ratio > 0.32) {
                result = 1;
            }

            try {
                //ImageIO.write(Ycomp, "jpg", new File(Ypath));
                //ImageIO.write(Cbcomp, "jpg", new File(Cbpath));
                ImageIO.write(Crcomp, "jpg", new File(Crpath));
                ImageIO.write(edgemap, "jpg", new File(edgepath));
                //ImageIO.write(test, "jpg", new File(exp));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;

        }

        private static int getPixel(BufferedImage input, int x, int y) {
            int p = input.getRGB(x, y);
            return p;
        }

        private static int rgbBounded(int val) {
            //Bounds between 0 and 255
            if (val > 255) {
                val = 255;
            }
            if (val < 0) {
                val = 0;
            }
            return val;
        }

        private static double getIntensity(int p) {
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = (p) & 0xFF;
            if (r == g && g == b) return r;
            return 0.299 * r + 0.587 * g + 0.114 * b;
        }

    }
}