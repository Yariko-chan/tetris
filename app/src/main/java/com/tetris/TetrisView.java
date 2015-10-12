package com.tetris;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by Diana on 25.09.2015.
 */
public class TetrisView extends SurfaceView implements SurfaceHolder.Callback{
    private static final String TAG = TetrisView.class.getSimpleName() ;

    private static final int FIELD_WIDTH = 10;
    private static final int FIELD_HEIGHT = 20;

    private TetrisThread drawThread;

    private int mCellSize;
    private Rect mFieldSize;
    private static boolean[][] mField;

    private void init() {
        getHolder().addCallback(this);
        mField = new boolean[FIELD_HEIGHT][FIELD_WIDTH];
//        mField[0][0] = true;
//        mField[0][1] = true;
//        mField[1][4] = true;
    }

    public TetrisView(Context context) {
        super(context);
        init();
    }

    public TetrisView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TetrisView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfCreated");
        drawThread = new TetrisThread(getHolder(), getResources());
        drawThread.setRunning(true);
        drawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (width/FIELD_WIDTH > height/FIELD_HEIGHT){
            mCellSize = height/FIELD_HEIGHT;
            mFieldSize = new Rect(0, 0, mCellSize*FIELD_WIDTH, height);
        } else {
            mCellSize = width/FIELD_WIDTH;
            mFieldSize = new Rect(0, 0, width, mCellSize*FIELD_HEIGHT);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        Log.d(TAG, "surfDestroyed");
        boolean retry = true;
        drawThread.setRunning(false);
        while (retry) {
            try {
                drawThread.join();
                retry = false;
            } catch (InterruptedException e) {
                // если не получилось, то будем пытаться еще и еще
            }
        }
    }

    public void moveFigure(boolean left) {
        drawThread.moveFigure(left);
    }

    public void rotate() {
        try {
            drawThread.rotateFigure();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class TetrisThread extends Thread {
        private boolean isRunning;
        private SurfaceHolder surfaceHolder;

        private int mState = STATE_START;
        private long mStartTime;
        private Figure mFigure;
        private int beginX;
        private int beginY;

        private final static int STATE_START = 0;
        private final static int STATE_RUNNING = 1;
        private final static int STATE_FINISH = 2;

        private final static int DEFAULT_X = 0;
        private final static int DEFAULT_Y = 4;

        public TetrisThread(SurfaceHolder holder, Resources res){
            surfaceHolder = holder;

        }

        public void setRunning(boolean run){
            isRunning = run;
        }

        @Override
        public void run() {
            super.run();
            Canvas canvas;
            while (isRunning){
                canvas = null;
                try {
                    // получаем объект Canvas и выполняем отрисовку
                    canvas = surfaceHolder.lockCanvas(null);
                    synchronized (surfaceHolder) {
                        switch (mState){
                            case STATE_START: start(canvas); break;
                            case STATE_RUNNING: fall(canvas);break;
                            case STATE_FINISH: finish(canvas); break;
                            default: break;
                        }
                    }
                }
                finally {
                    if (canvas != null) {
                        // отрисовка выполнена. выводим результат на экран
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }

        }

        private void finish(Canvas canvas) {
            drawFigureOnField(mFigure, beginX, beginY);
            drawField(canvas);
            mState = STATE_START;
        }

        private void fall(Canvas canvas) {
            long lastTime = (System.currentTimeMillis() - mStartTime)/1000;
            if (isCanFall(mFigure, beginX, beginY)){
//            if (lastTime>17){
                beginX = (int) lastTime;

                drawField(canvas);
//                drawFigure(canvas, mFigure, beginX, beginY);
                fillFigure(canvas, mFigure, beginX, beginY);
            } else {
                mState = STATE_FINISH;
            }
        }

        private void start(Canvas canvas) {
            mStartTime = System.currentTimeMillis();

            mFigure = new Figure();
            beginX = DEFAULT_X;
            beginY = DEFAULT_Y;

            drawField(canvas);
            drawFigure(canvas, mFigure, beginX, beginY);
            mState = STATE_RUNNING;
        }


        public void moveFigure(boolean left) {
            if (left) beginY -=1;
            else beginY +=1;
        }

        public void rotateFigure() throws Exception {
            mFigure.rotate();
        }
    }

    private void drawField(Canvas canvas) {

        Paint gridPaint = new Paint();
        gridPaint.setStyle(Paint.Style.FILL);
        gridPaint.setColor(getResources().getColor(R.color.light_brown));
        canvas.drawRect(mFieldSize, gridPaint);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setColor(getResources().getColor(R.color.dark_brown));

        for (int i = 0; i<FIELD_HEIGHT; i++) canvas.drawLine(mFieldSize.left, i*mCellSize, mFieldSize.right, i*mCellSize, gridPaint);
        for (int i = 0; i<FIELD_WIDTH; i++) canvas.drawLine(i*mCellSize, mFieldSize.top, i*mCellSize, mFieldSize.bottom, gridPaint);

        for (int i = 0; i < mField.length; i++){
            for (int j = 0; j < mField[0].length; j++) {
                if (mField[i][j]) fillCell(canvas, i, j);
            }
        }
    }

    //need to initialize paints once while creating thread
    private void fillCell(Canvas canvas, int x, int y) {
        Paint fillPaint = new Paint();
        fillPaint.setColor(getResources().getColor(R.color.dark_brown));
        fillPaint.setStyle(Paint.Style.FILL);

        canvas.drawRect(new Rect(y*mCellSize, x*mCellSize, (y+1)*mCellSize, (x+1)*mCellSize), fillPaint);
    }

    private void fillFigure(Canvas canvas, Figure figure, int beginX, int beginY){
        int[] x = figure.getX(beginX);
        int[] y = figure.getY(beginY);
        for (int i = 0; i<x.length; i++){
            fillCell(canvas, x[i], y[i]);
        }
    }

    private void drawFigure(Canvas canvas, Figure figure, int x, int y){
        // x - string, y - column
        switch (figure.mType){
            case Figure.I:
                if (figure.mVariation == Figure.UP || figure.mVariation == Figure.DOWN ){
                    fillCell(canvas,   x, y);
                    fillCell(canvas, x+1, y);
                    fillCell(canvas, x+2, y);
                    fillCell(canvas, x + 3, y);
                } else {
                    fillCell(canvas, x, y - 1);
                    fillCell(canvas, x, y);
                    fillCell(canvas, x, y + 1);
                    fillCell(canvas, x, y + 2);}
                break;
            case Figure.J:
                switch (figure.mVariation){
                case Figure.LEFT:
                    fillCell(canvas,   x, y-1);
                    fillCell(canvas,   x, y);
                    fillCell(canvas,   x, y+1);
                    fillCell(canvas, x+1, y+1);
                    break;
                case Figure.DOWN:
                    fillCell(canvas,   x, y);
                    fillCell(canvas,   x, y-1);
                    fillCell(canvas, x+1, y-1);
                    fillCell(canvas, x+2, y-1);
                    break;
                case Figure.RIGHT:
                    fillCell(canvas, x + 1, y - 1);
                    fillCell(canvas, x + 2, y - 1);
                    fillCell(canvas, x + 2, y);
                    fillCell(canvas, x + 2, y + 1);
                    break;
                default:
                    fillCell(canvas,   x, y+1);
                    fillCell(canvas, x+1, y+1);
                    fillCell(canvas, x+2, y+1);
                    fillCell(canvas, x+2, y);
                    break;
                }break;
            case Figure.L:
                switch (figure.mVariation){
                    case Figure.LEFT:
                        fillCell(canvas, x + 2, y - 1);
                        fillCell(canvas, x + 2, y);
                        fillCell(canvas, x + 2, y + 1);
                        fillCell(canvas, x + 1, y + 1);
                        break;
                    case Figure.DOWN:
                        fillCell(canvas, x, y);
                        fillCell(canvas, x, y + 1);
                        fillCell(canvas, x + 1, y + 1);
                        fillCell(canvas, x + 2, y + 1);
                        break;
                    case Figure.RIGHT:
                        fillCell(canvas, x + 1, y - 1);
                        fillCell(canvas, x, y - 1);
                        fillCell(canvas, x, y);
                        fillCell(canvas, x, y + 1);
                        break;
                    default:
                        fillCell(canvas, x, y - 1);
                        fillCell(canvas, x + 1, y - 1);
                        fillCell(canvas, x + 2, y - 1);
                        fillCell(canvas, x + 2, y);
                        break;
                }break;
            case Figure.O:
                fillCell(canvas, x, y);
                fillCell(canvas, x, y + 1);
                fillCell(canvas, x + 1, y);
                fillCell(canvas, x + 1, y + 1);
                break;
            case Figure.S:
                switch (figure.mVariation){
                    case Figure.LEFT:
                        fillCell(canvas, x, y + 1);
                        fillCell(canvas, x, y);
                        fillCell(canvas, x + 1, y);
                        fillCell(canvas, x + 1, y - 1);
                        break;
                    case Figure.DOWN:
                        fillCell(canvas, x, y - 1);
                        fillCell(canvas, x + 1, y - 1);
                        fillCell(canvas, x + 1, y);
                        fillCell(canvas, x + 2, y);
                        break;
                    case Figure.RIGHT:
                        fillCell(canvas, x + 1, y + 1);
                        fillCell(canvas, x + 1, y);
                        fillCell(canvas, x + 2, y);
                        fillCell(canvas, x + 2, y - 1);
                        break;
                    default:
                        fillCell(canvas, x, y);
                        fillCell(canvas, x + 1, y);
                        fillCell(canvas, x + 1, y + 1);
                        fillCell(canvas, x + 2, y + 1);
                        break;
                }break;
            case Figure.T:
                fillCell(canvas, x + 1, y);
                switch (figure.mVariation){
                    case Figure.LEFT:
                        fillCell(canvas, x, y + 1);
                        fillCell(canvas, x + 1, y + 1);
                        fillCell(canvas, x + 2, y + 1);
                        break;
                    case Figure.DOWN:
                        fillCell(canvas, x, y - 1);
                        fillCell(canvas, x, y);
                        fillCell(canvas, x, y + 1);
                        break;
                    case Figure.RIGHT:
                        fillCell(canvas, x, y - 1);
                        fillCell(canvas, x + 1, y - 1);
                        fillCell(canvas, x + 2, y - 1);
                        break;
                    default:
                        fillCell(canvas, x + 2, y - 1);
                        fillCell(canvas, x + 2, y);
                        fillCell(canvas, x + 2, y + 1);
                        break;
                }break;
            case Figure.Z:
                switch (figure.mVariation){
                    case Figure.LEFT:
                        fillCell(canvas, x + 1, y - 1);
                        fillCell(canvas, x + 1, y);
                        fillCell(canvas, x + 2, y);
                        fillCell(canvas, x + 2, y + 1);
                        break;
                    case Figure.DOWN:
                        fillCell(canvas, x, y + 1);
                        fillCell(canvas, x + 1, y + 1);
                        fillCell(canvas, x + 1, y);
                        fillCell(canvas, x + 2, y);
                        break;
                    case Figure.RIGHT:
                        fillCell(canvas, x, y - 1);
                        fillCell(canvas, x, y);
                        fillCell(canvas, x + 1, y);
                        fillCell(canvas, x + 1, y + 1);
                        break;
                    default:
                        fillCell(canvas, x, y);
                        fillCell(canvas, x + 1, y);
                        fillCell(canvas, x + 1, y - 1);
                        fillCell(canvas, x + 2, y - 1);
                        break;
                }break;
            default:break;
        }
    }

    private void drawFigureOnField(Figure figure, int beginX, int beginY){
        int[] x = figure.getX(beginX);
        int[] y = figure.getY(beginY);
        for (int i = 0; i<x.length; i++){
            mField[x[i]][y[i]] = true;
        }
    }

    private boolean isCanFall(Figure figure, int beginX, int beginY) {
        int[] x = figure.getX(beginX);
        int[] y = figure.getY(beginY);
//        boolean isFree = true;
        for (int i = 0; i<x.length; i++){
            if ((mField.length <= x[i]+1) || (mField[x[i]+1][y[i]] == true)) return false;
        }
        return true;
    }

    private void drawFigureToField(Figure figure, int x, int y){
        // x - string, y - column
        switch (figure.mType){
            case Figure.I:
                if (figure.mVariation == Figure.UP || figure.mVariation == Figure.DOWN ){
                    mField[x][y] = true;
                    mField[x+1][y] = true;
                    mField[x+2][y] = true;
                    mField[x+3][y] = true;
                } else {
                    mField[x][y-1] = true;
                    mField[x][y] = true;
                    mField[x][y+1] = true;
                    mField[x][y+2] = true;
                }
                break;
            case Figure.J:
                switch (figure.mVariation){
                    case Figure.LEFT:
                        mField[x][y-1] = true;
                        mField[x][y] = true;
                        mField[x][y+1] = true;
                        mField[x+1][y+1] = true;
                        break;
                    case Figure.DOWN:
                        mField[x][y] = true;
                        mField[x][y-1] = true;
                        mField[x+1][y-1] = true;
                        mField[x+2][y-1] = true;
                        break;
                    case Figure.RIGHT:
                        mField[x+1][y-1] = true;
                        mField[x+2][y-1] = true;
                        mField[x+2][y] = true;
                        mField[x+2][y+1] = true;
                        break;
                    default:
                        mField[x][y+1] = true;
                        mField[x+1][y+1] = true;
                        mField[x+2][y+1] = true;
                        mField[x+2][y] = true;
                        break;
                }break;
            case Figure.L:
                switch (figure.mVariation){
                    case Figure.LEFT:
                        mField[x+2][y-1] = true;
                        mField[x+2][y] = true;
                        mField[x+2][y+1] = true;
                        mField[x+1][y+1] = true;
                        break;
                    case Figure.DOWN:
                        mField[x][y] = true;
                        mField[x][y+1] = true;
                        mField[x+1][y+1] = true;
                        mField[x+2][y+1] = true;
                        break;
                    case Figure.RIGHT:
                        mField[x+1][y-1] = true;
                        mField[x][y-1] = true;
                        mField[x][y] = true;
                        mField[x][y+1] = true;
                        break;
                    default:
                        mField[x][y-1] = true;
                        mField[x+1][y-1] = true;
                        mField[x+2][y-1] = true;
                        mField[x+2][y] = true;
                        break;
                }break;
            case Figure.O:
                mField[x][y] = true;
                mField[x][y+1] = true;
                mField[x+1][y] = true;
                mField[x+1][y+1] = true;
                break;
            case Figure.S:
                switch (figure.mVariation){
                    case Figure.LEFT:
                        mField[x][y+1] = true;
                        mField[x][y] = true;
                        mField[x+1][y] = true;
                        mField[x+1][y-1] = true;
                        break;
                    case Figure.DOWN:
                        mField[x][y-1] = true;
                        mField[x+1][y-1] = true;
                        mField[x+1][y] = true;
                        mField[x+2][y] = true;
                        break;
                    case Figure.RIGHT:
                        mField[x+1][y+1] = true;
                        mField[x+1][y] = true;
                        mField[x+2][y] = true;
                        mField[x+2][y-1] = true;
                        break;
                    default:
                        mField[x][y] = true;
                        mField[x+1][y] = true;
                        mField[x+1][y+1] = true;
                        mField[x+2][y+1] = true;
                        break;
                }break;
            case Figure.T:
                mField[x+1][y] = true;
                switch (figure.mVariation){
                    case Figure.LEFT:
                        mField[x][y+1] = true;
                        mField[x+1][y+1] = true;
                        mField[x+2][y+1] = true;
                        break;
                    case Figure.DOWN:
                        mField[x][y-1] = true;
                        mField[x][y] = true;
                        mField[x][y+1] = true;
                        break;
                    case Figure.RIGHT:
                        mField[x][y-1] = true;
                        mField[x+1][y-1] = true;
                        mField[x+2][y-1] = true;
                        break;
                    default:
                        mField[x+2][y-1] = true;
                        mField[x+2][y] = true;
                        mField[x+2][y+1] = true;
                        break;
                }break;
            case Figure.Z:
                switch (figure.mVariation){
                    case Figure.LEFT:
                        mField[x+1][y-1] = true;
                        mField[x+1][y] = true;
                        mField[x+2][y] = true;
                        mField[x+2][y+1] = true;
                        break;
                    case Figure.DOWN:
                        mField[x][y+1] = true;
                        mField[x+1][y+1] = true;
                        mField[x+1][y] = true;
                        mField[x+2][y] = true;
                        break;
                    case Figure.RIGHT:
                        mField[x][y-1] = true;
                        mField[x][y] = true;
                        mField[x+1][y] = true;
                        mField[x+1][y+1] = true;
                        break;
                    default:
                        mField[x][y] = true;
                        mField[x+1][y] = true;
                        mField[x+1][y-1] = true;
                        mField[x+2][y-1] = true;
                        break;
                }break;
            default:break;
        }
    }

    private class Figure {
        public int mType;
        public int mVariation;

        //types
        protected final static int I = 0;
        protected final static int J = 1;
        protected final static int L = 2;
        protected final static int O = 3;
        protected final static int S = 4;
        protected final static int T = 5;
        protected final static int Z = 6;

        //variations
        protected final static int UP = 1; //default
        protected final static int LEFT = 2;
        protected final static int DOWN = 3;
        protected final static int RIGHT = 4;

        public Figure(){
            mType = (int)(Math.random()*7);
            mVariation = (int)(Math.random()*4+1);
            //random mFigure in random orientation
        }

        public Figure(int type) {
            mType = type;
            mVariation = UP;
            //or random?
        }

        public Figure(int type, int variation) {
            mType = type;
            mVariation = variation;
        }

        public void rotate() throws Exception {
            switch (mVariation){
                case UP: mVariation = LEFT; break;
                case LEFT: mVariation = DOWN; break;
                case DOWN: mVariation = RIGHT; break;
                case RIGHT: mVariation = UP; break;
                default: throw new Exception("Undefined variation of mFigure with value of " + mVariation);

            }
        }

        //array of x for cells of figure relatevely begin x from parameter
        public int[] getX(int x){
            switch (mType) {
                case Figure.J:
                    switch (mVariation) {
                        case Figure.LEFT: return new int[]{x, x, x, x + 1};
                        case Figure.DOWN: return new int[]{x, x, x + 1, x + 2};
                        case Figure.RIGHT: return new int[]{x + 1, x + 2, x + 2, x + 2};
                        default: return new int[]{x, x + 1, x + 2, x + 2};
                    }
                case Figure.L:
                    switch (mVariation) {
                        case Figure.LEFT: return new int[]{x + 2, x + 2, x + 2, x + 1};
                        case Figure.DOWN: return new int[]{x, x, x + 1, x + 2};
                        case Figure.RIGHT: return new int[]{x + 1, x, x, x};
                        default: return new int[]{x, x + 1, x + 2, x + 2};
                    }
                case Figure.O: return new int[]{x, x, x + 1, x + 1};
                case Figure.S:
                    switch (mVariation) {
                        case Figure.LEFT: return new int[]{x, x, x + 1, x + 1};
                        case Figure.DOWN: return new int[]{x, x + 1, x + 1, x + 2};
                        case Figure.RIGHT: return new int[]{x + 1, x + 1, x + 2, x + 2};
                        default: return new int[]{x, x + 1, x + 1, x + 2};
                    }
                case Figure.T:
                    switch (mVariation) {
                        case Figure.LEFT: return new int[]{x + 1, x, x + 1, x + 2};
                        case Figure.DOWN: return new int[]{x + 1, x, x, x};
                        case Figure.RIGHT: return new int[]{x + 1, x, x + 1, x + 2};
                        default: return new int[]{x + 1, x + 2, x + 2, x + 2};
                    }
                case Figure.Z:
                    switch (mVariation) {
                        case Figure.LEFT: return new int[]{x + 1, x + 1, x + 2, x + 2};
                        case Figure.DOWN: return new int[]{x, x + 1, x + 1, x + 2};
                        case Figure.RIGHT: return new int[]{x, x, x + 1, x + 1};
                        default: return new int[]{x, x + 1, x + 1, x + 2};
                    }
                default://for I
                    if (mVariation == Figure.UP || mVariation == Figure.DOWN) {
                        return new int[]{x, x + 1, x + 2, x + 3};
                    } else {
                        return new int[]{x, x, x, x};
                    }
            }
        }

        //array of y for cells relatevely begin x from parameter
        public int[] getY(int y){
            switch (mType){
                case Figure.J:
                    switch (mVariation){
                        case Figure.LEFT: return new int[]{y-1, y, y+1, y+1};
                        case Figure.DOWN: return new int[]{y, y-1, y-1, y-1};
                        case Figure.RIGHT: return new int[]{y-1, y-1, y, y+1};
                        default: return new int[]{y+1, y+1, y+1, y};
                    }
                case Figure.L:
                    switch (mVariation){
                        case Figure.LEFT: return new int[]{y-1, y, y+1, y+1};
                        case Figure.DOWN: return new int[]{y, y+1, y+1, y+1};
                        case Figure.RIGHT: return new int[]{y-1, y-1, y, y+1};
                        default: return new int[]{y-1, y-1, y-1, y};
                    }
                case Figure.O: return new int[]{y, y+1, y, y+1};
                case Figure.S:
                    switch (mVariation){
                        case Figure.LEFT: return new int[]{y+1, y, y, y-1};
                        case Figure.DOWN: return new int[]{y-1, y-1, y, y};
                        case Figure.RIGHT: return new int[]{y+1, y, y, y-1};
                        default: return new int[]{y, y, y+1, y+1};
                    }
                case Figure.T:
                    switch (mVariation){
                        case Figure.LEFT: return new int[]{y, y+1, y+1, y+1};
                        case Figure.DOWN: return new int[]{y, y-1, y, y+1};
                        case Figure.RIGHT: return new int[]{y, y-1, y-1, y-1};
                        default: return new int[]{y, y-1, y, y+1};
                    }
                case Figure.Z:
                    switch (mVariation){
                        case Figure.LEFT: return new int[]{y-1, y, y, y+1};
                        case Figure.DOWN: return new int[]{y+1, y+1, y, y};
                        case Figure.RIGHT: return new int[]{y-1, y, y, y+1};
                        default: return new int[]{y, y, y-1, y-1};
                    }
                default:
                    if (mVariation == Figure.UP || mVariation == Figure.DOWN ){
                        return new int[]{y, y, y, y};
                    } else {
                        return new int[]{y-1, y, y+1, y+2};
                    }
            }
        }
    }
}
