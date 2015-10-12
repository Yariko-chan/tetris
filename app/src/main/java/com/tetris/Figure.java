package com.tetris;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

/**
 * Created by Diana on 28.09.2015.
 */
public class Figure {
    Type mType;
//    private Point mBegin;
    private byte mVariation;

    enum Type {I, L, J, T, O, S, Z};
    enum Variation{UP, LEFT, DOWN, RIGHT};
    enum State {START, FALLING, FINISH};

    public Figure(){

    }
    public Figure(Type type, byte variation){
        mType = type;
        mVariation = variation;
    }

    public void rotate(){

    }

    public void drawFigure(Canvas canvas, Point begin){

    }


//    private void drawFigure(Canvas canvas, Type mFigure, Variation v, Point centerCell){
//        Paint sqPaint = new Paint();
//        sqPaint.setColor(Color.RED);
//        sqPaint.setStyle(Paint.Style.FILL);
//
//        switch (mFigure){
//            case I:break;
//            case L:break;
//            case J:break;
//            case O:
//                canvas.drawRect(centerCell.x, centerCell.y, centerCell.x+ mCellSize, centerCell.y+ mCellSize, sqPaint);
//                canvas.drawRect(centerCell.x+ mCellSize, centerCell.y, centerCell.x+ mCellSize *2, centerCell.y+ mCellSize, sqPaint);
//                canvas.drawRect(centerCell.x, centerCell.y+ mCellSize, centerCell.x+ mCellSize, centerCell.y+ mCellSize *2, sqPaint);
//                canvas.drawRect(centerCell.x+ mCellSize, centerCell.y+ mCellSize, centerCell.x+ mCellSize *2, centerCell.y+ mCellSize *2, sqPaint);
//                break;
//            case S:break;
//            case T:break;
//            case Z:break;
//            default:
//        }
//    }
}
