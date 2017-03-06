package com.clam.stickycircleview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Created by clam314 on 2017/3/3
 */

public class StickyCircleView extends View{
    private final static float DEFAULT_RADIUS = 50f;
    private final static float DEFAULT_PADDING = 20f;

    private static final long LOADING_DURATION = 2000;
    private static final long STICKY_DURATION = 300;

    //移动的最大距离
    private float MaxMoveDistance = 1000f;
    private int viewWidth,viewHeight;
    private int circleColor = Color.parseColor("#00ffad");
    private int loadPathColor = Color.WHITE;

    private Circle circleStart, circleEnd;
    private PointF pStartA,pStartB,pEndA,pEndB,pControlO, pControlP;
    private PointF downPoint,movePoint;

    private Paint mBezierPaint, mLoadPaint;
    private Path mBezierPath,mLoadPath;
    private PathMeasure pathMeasure;

    private ValueAnimator stickyAnimator,loadAnimator;
    private FloatEvaluator evaluator;

    private float mLoadAnimatorValue;
    private float mScale;

    private boolean loading = false;
    private OnReloadListener mReloadListener;

    public StickyCircleView(Context context) {
        super(context);
        initAll();
    }

    public StickyCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAll();
    }

    public StickyCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAll();
    }

    private void initAll(){
        initCircleAndPoints();
        initPaint();
        initPath();
        initAnimation();
    }

    private void initCircleAndPoints(){
        circleStart = new Circle(0,0, DEFAULT_RADIUS);
        circleEnd = new Circle(0,0, DEFAULT_RADIUS);

        pStartA = new PointF();
        pStartB = new PointF();
        pEndA = new PointF();
        pEndB = new PointF();
        pControlP = new PointF();
        pControlO = new PointF();
        downPoint = new PointF();
        movePoint = new PointF();
    }

    private void initPaint(){
        mBezierPaint = new Paint();
        mBezierPaint.setAntiAlias(true);
        mBezierPaint.setStrokeWidth(1);
        mBezierPaint.setStyle(Paint.Style.FILL);
        mBezierPaint.setColor(circleColor);

        mLoadPaint = new Paint();
        mLoadPaint.setAntiAlias(true);
        mLoadPaint.setStrokeWidth(5);
        mLoadPaint.setStyle(Paint.Style.STROKE);
        mLoadPaint.setStrokeCap(Paint.Cap.ROUND);
        mLoadPaint.setColor(loadPathColor);
    }

    private void initPath(){
        mBezierPath = new Path();

        pathMeasure = new PathMeasure();
        mLoadPath = new Path();
        float loadCircleRadius = DEFAULT_RADIUS - DEFAULT_PADDING;
        RectF circle = new RectF(-loadCircleRadius, -loadCircleRadius, loadCircleRadius, loadCircleRadius);
        mLoadPath.addArc(circle, 0, 359.9f);
    }

    private void initAnimation(){
        evaluator = new FloatEvaluator();
        stickyAnimator = new ValueAnimator();
        stickyAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        stickyAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //让最后移动的点按直线移动到原来按下的点的位置
                float newDistance = (float) animation.getAnimatedValue();
                float distance = getDistanceBetweenTwoPoints(downPoint.x,downPoint.y,movePoint.x,movePoint.y);
                float cos = (movePoint.x - downPoint.x)/distance;
                float sin = (movePoint.y - downPoint.y)/distance;
                movePoint.x = downPoint.x + newDistance * cos;
                movePoint.y = downPoint.y + newDistance * sin;
                invalidate();
            }
        });
        stickyAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(loading){
                    loadAnimator.start();
                    if(mReloadListener != null) mReloadListener.onReload();
                }
            }
        });

        loadAnimator = ValueAnimator.ofFloat(0,1).setDuration(LOADING_DURATION);
        loadAnimator.setRepeatCount(-1);//一直循环
        loadAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mLoadAnimatorValue = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
    }

    public void setOnReloadListener(OnReloadListener listener){
        this.mReloadListener = listener;
    }

    public boolean isLoading(){
        return loading;
    }

    public void stopReload(){
        if(loadAnimator.isRunning()){
            loadAnimator.cancel();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        circleEnd.centerPoint.x = circleStart.centerPoint.x = viewWidth/2;
        circleEnd.centerPoint.y = circleStart.centerPoint.y = 50f + DEFAULT_RADIUS;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //关闭硬件加速，否则部分path的绘制不生效
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);

        //根据按下的和滑动的点两个点的距离计算，开始圆和拉出圆的中心坐标以及半径
        calculateCircleSize();

        canvas.drawCircle(circleStart.centerPoint.x, circleStart.centerPoint.y, circleStart.radius, mBezierPaint);
        canvas.drawCircle(circleEnd.centerPoint.x, circleEnd.centerPoint.y, circleEnd.radius, mBezierPaint);

        if(calculateBezierCurve(circleStart,circleEnd)){
            drawBezierCurves(canvas);//绘制两圆间的贝塞尔曲线
        }


        if(loadAnimator.isRunning()){
            drawLoading(canvas);//绘制旋转时，中心的圆弧
        }else {
            drawLoadingNormal(canvas);//绘制中心的圆弧和箭头
        }

    }

    private void drawBezierCurves(Canvas canvas){
        mBezierPath.reset();
        mBezierPath.moveTo(pStartA.x,pStartA.y);
        mBezierPath.quadTo(pControlO.x, pControlO.y, pEndA.x, pEndA.y);
        mBezierPath.lineTo(pEndB.x, pEndB.y);
        mBezierPath.quadTo(pControlP.x, pControlP.y, pStartB.x, pStartB.y);
        mBezierPath.close();
        canvas.drawPath(mBezierPath, mBezierPaint);
    }

    private void drawLoadingNormal(Canvas canvas){
        //这里包含对画布坐标系的转换，快照一下，防止对影响后续绘制
        canvas.save();
        //将画布中心移到开始圆的中心
        canvas.translate(circleStart.centerPoint.x,circleStart.centerPoint.y);
        //根据移动的距离比例，对画布缩小和旋转
        canvas.scale(1 - mScale,1 - mScale);
        canvas.rotate(360 * mScale);

        pathMeasure.setPath(mLoadPath,false);//将中心圆圈的path和pathMeasure关联
        float[] pos = new float[2];
        float[] tan = new float[2];
        float stop = pathMeasure.getLength() * 0.75f;
        float start = 0;
        pathMeasure.getPosTan(stop,pos,tan);//获取截取圆弧的结束点的坐标和方向趋势
        //根据tan获取旋转的角度，用于旋转后面绘制的箭头
        float degrees =(float)(Math.atan2(tan[1],tan[0])*180/Math.PI);

        Matrix matrix = new Matrix();
        Path triangle = new Path();
        //绘制箭头，此时的箭头的顶点坐标还在原点
        triangle.moveTo(pos[0] - 5, pos[1] + 5);
        triangle.lineTo(pos[0],pos[1]);
        triangle.lineTo(pos[0] + 5, pos[1] + 5);
        triangle.close();
        //将箭头移动到圆弧结束点的位置并旋转
        matrix.setRotate(degrees+90, pos[0],pos[1]);

        Path showPath = new Path();
        //前面的箭头添加将要绘制的路径里面
        showPath.addPath(triangle,matrix);
        //截取圆圈从起始点到结束的圆弧并添加到要绘制的path中，true代表不将截取的圆弧的起点移动到之前path的最后一个点上
        pathMeasure.getSegment(start,stop,showPath,true);

        canvas.drawPath(showPath, mLoadPaint);
        canvas.restore();
    }

    private void drawLoading(Canvas canvas){
        //基本和绘制一般状态的时候一样，除了截取的起点和终点需要动态的计算
        canvas.save();
        canvas.translate(circleStart.centerPoint.x, circleStart.centerPoint.y);
        canvas.scale(1 - mScale,1 - mScale);
        pathMeasure.setPath(mLoadPath,false);
        Path newPath = new Path();
        float stop = pathMeasure.getLength() * mLoadAnimatorValue;
        float start = (float)(stop - (0.5 - Math.abs(mLoadAnimatorValue - 0.5)) * 200f);
        pathMeasure.getSegment(start,stop,newPath,true);
        canvas.drawPath(newPath, mLoadPaint);
        canvas.restore();
    }

    private void calculateCircleSize(){
        float mMoveDistance = getDistanceBetweenTwoPoints(downPoint.x,downPoint.y,movePoint.x,movePoint.y);
        //两圆重合无需再计算
        if(mMoveDistance <= 0) return;
        mScale = mMoveDistance/MaxMoveDistance;
        //开始圆按比例缩小
        circleStart.radius = DEFAULT_RADIUS * (1- mScale);
        //拉出圆按比例放大
        circleEnd.radius = DEFAULT_RADIUS * mScale;

        //开始圆的位置不变，拉出圆的位置根据滑动的距离移动
        circleEnd.centerPoint.x = circleStart.centerPoint.x + movePoint.x - downPoint.x;
        circleEnd.centerPoint.y = circleStart.centerPoint.y + movePoint.y - downPoint.y;
    }

    private boolean calculateBezierCurve(Circle circleStart, Circle circleEnd){
        float startRadius = circleStart.radius;
        float endRadius = circleEnd.radius;
        float startX = circleStart.centerPoint.x;
        float startY = circleStart.centerPoint.y;
        float endX= circleEnd.centerPoint.x;
        float endY = circleEnd.centerPoint.y;

        float mCircleDistance = getDistanceBetweenTwoPoints(startX,startY,endX,endY);
        //两个圆重合就无需要绘制连接曲线
        if(mCircleDistance == 0){
            return false;
        }

        float cos = (startX - endX)/mCircleDistance;
        float sin = (startY - endY)/mCircleDistance;

        float ax = startX - startRadius * sin;
        float ay = startY + startRadius * cos;
        pStartA.x = ax;
        pStartA.y = ay;

        float bx = startX + startRadius * sin;
        float by = startY - startRadius * cos;
        pStartB.x = bx;
        pStartB.y = by;

        float cx = endX - endRadius * sin;
        float cy = endY + endRadius * cos;
        pEndA.x = cx;
        pEndA.y = cy;

        float dx = endX + endRadius * sin;
        float dy = endY - endRadius * cos;
        pEndB.x = dx;
        pEndB.y = dy;

        float ox = cx + mCircleDistance /2 * cos;
        float oy = cy + mCircleDistance /2 * sin;
        pControlO.x = ox;
        pControlO.y = oy;

        float px = dx + mCircleDistance /2 * cos;
        float py = dy + mCircleDistance /2 * sin;
        pControlP.x = px;
        pControlP.y = py;

        return true;
    }

    private static float getDistanceBetweenTwoPoints(float p1x, float ply, float p2x, float p2y){
        return (float) Math.sqrt(Math.pow(p1x - p2x, 2) + Math.pow(ply - p2y, 2));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        //动画执行时，无需改变两点的坐标
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(!stickyAnimator.isRunning() && !loadAnimator.isRunning()){
                    downPoint.x = x;
                    downPoint.y = y;
                    movePoint.set(downPoint);
                    resetLoadAnimator();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(!stickyAnimator.isRunning() && !loadAnimator.isRunning() && !loading){
                    movePoint.x = x;
                    movePoint.y = y;
                    float distanceMove = getDistanceBetweenTwoPoints(downPoint.x,downPoint.y,movePoint.x,movePoint.y);
                    //滑动距离在动作范围内，则开始执行回滚动画和loading动画
                    if(inLoadArea(distanceMove)){
                        loading = true;
                        executeAnimator(distanceMove);
                    }
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(!stickyAnimator.isRunning() && !loadAnimator.isRunning() && !loading){
                    movePoint.x = x;
                    movePoint.y = y;
                    float distanceUp = getDistanceBetweenTwoPoints(downPoint.x,downPoint.y,movePoint.x,movePoint.y);
                    //滑动距离在动作范围内，则开始执行回滚动画和loading动画，否则只开始回滚动画
                    if(inLoadArea(distanceUp)){
                        loading = true;
                    }
                    executeAnimator(distanceUp);
                }
                break;
        }
        return true;
    }

    private void resetLoadAnimator(){
        loading = false;
    }

    private boolean inLoadArea(float distance){
        return distance <= MaxMoveDistance*0.75 && distance >= MaxMoveDistance * 0.33;
    }

    private void executeAnimator(float distance){
        //两个圆重合时无需回滚
        if(distance == 0) return;
        //只有在设置了变化的值后，设置TypeEvaluator才有效，否则会报空指针
        stickyAnimator.setObjectValues(distance,0);
        stickyAnimator.setEvaluator(evaluator);
        stickyAnimator.setDuration(STICKY_DURATION);
        stickyAnimator.start();
    }

    static class Circle{
        PointF centerPoint;
        float radius;
        Circle(float centerX,float centerY,float radius){
            centerPoint = new PointF(centerX,centerY);
            this.radius = radius;
        }
    }

    public interface OnReloadListener{
        void onReload();
    }
}
