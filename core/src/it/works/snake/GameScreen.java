package it.works.snake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen extends ScreenAdapter implements GestureDetector.GestureListener {

    //Game state data
    private enum STATE {
        PLAYING, GAME_OVER
    }

    private STATE state = STATE.PLAYING;
    private int score;

    //General data
    private static final float WORLD_WIDTH = 640;
    private static final float WORLD_HEIGHT = 480;

    private Viewport viewport;
    private Camera camera;
    private GestureDetector gestureDetector;

    private BitmapFont bitmapFont;
    private GlyphLayout glyphLayout = new GlyphLayout();
    private SpriteBatch batch;
    private Array<BodyPart> bodyPartArray = new Array<BodyPart>();
    private ShapeRenderer shapeRenderer;
    private static final int GRID_CELL = 32;
    private boolean directionSet = false;
    private static final int POINTS_PER_APPLE = 10;

    //Directions
    private final static int RIGHT = 0;
    private final static int LEFT = 1;
    private final static int UP = 2;
    private final static int DOWN = 3;

    private final static int ANGLE_RIGHT = 90;
    private final static int ANGLE_LEFT = 270;
    private final static int ANGLE_UP = 180;
    private final static int ANGLE_DOWN = 0;

    //Frame timings
    private static final float MOVE_TIME = 0.18F;
    private float timer = MOVE_TIME;

    //Textures
    private Texture snakeHeadTexture = new Texture(Gdx.files.internal("snakehead.png"));
    private Texture appleTexture = new Texture(Gdx.files.internal("apple.png"));
    private Texture snakeBodyTexture = new Texture(Gdx.files.internal("snakebody.png"));
    private Texture grassTexture = new Texture(Gdx.files.internal("grass.png"));

    private TextureRegion snakeHead;
    private TextureRegion apple;
    private TextureRegion grass;

    //Snake data
    private int snakePositionX = 0;
    private int snakePositionY = 0;

    private int snakeXBeforeUpdate = 0;
    private int snakeYBeforeUpdate = 0;

    private int snakeDirection = RIGHT;
    private int snakeAngle = 90;

    private static final int SNAKE_MOVEMENT = 32;
    private static final int SNAKE_WIDTH = 32;
    private static final int SNAKE_HEIGHT = 32;

    //Apple data
    private boolean appleAvailable = false;

    private int applePositionX = 0;
    private int applePositionY = 0;

    private static final int APPLE_WIDTH = 32;
    private static final int APPLE_HEIGHT = 32;

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        gestureDetector = new GestureDetector(this);
        Gdx.input.setInputProcessor(gestureDetector);
        batch = new SpriteBatch();
        snakeHead = new TextureRegion(snakeHeadTexture);
        apple = new TextureRegion(appleTexture);
        grass = new TextureRegion(grassTexture);
        shapeRenderer = new ShapeRenderer();
        bitmapFont = new BitmapFont(Gdx.files.internal("snakeFont.fnt"));
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);
    }

    @Override
    public void render(float delta) {
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        super.render(delta);
        switch (state) {
            case PLAYING: {
                queryInput();
                updateSnake(delta);
                checkAppleCollision();
                checkAndPlaceApple();
            }
            break;
            case GAME_OVER: {
                checkForRestart();
            }
            break;
        }
        clearScreen();
        draw();
        drawGrid();
        if (state == STATE.GAME_OVER) drawGameOverText(score);
    }

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {

        if (state == state.GAME_OVER) doRestart();

        switch (snakeDirection) {
            case RIGHT:
                updateDirection(DOWN);
                break;
            case LEFT:
                updateDirection(UP);
                break;
            case UP:
                updateDirection(RIGHT);
                break;
            case DOWN:
                updateDirection(LEFT);
                break;
        }
        return false;
    }

    @Override
    public boolean longPress(float x, float y) {
        return false;
    }

    @Override
    public boolean fling(float velocityX, float velocityY, int button) {
        return false;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        return false;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        return false;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }

    @Override
    public void pinchStop() {

    }

    private void drawGameOverText(int score) {
        String string = "GAME OVER! \n YOUR FINAL SCORE: " + score + " \nTAP TO TRY AGAIN";
        batch.begin();
        glyphLayout.setText(bitmapFont, string);
        bitmapFont.draw(batch, string, (viewport.getWorldWidth() -
                glyphLayout.width) / 2, (viewport.getWorldHeight() - glyphLayout.height));
        batch.end();
    }

    private void updateSnake(float delta) {
        timer -= delta;
        if (timer <= 0) {
            timer = MOVE_TIME;
            moveSnake();
            checkForOutOfBounds();
            updateBodyPartsPosition();
            checkSnakeBodyCollision();
            directionSet = false;
        }
    }

    private void draw() {
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(grass, 0, 0);
        batch.draw(snakeHead, snakePositionX, snakePositionY, SNAKE_WIDTH / 2, SNAKE_HEIGHT / 2, SNAKE_WIDTH, SNAKE_HEIGHT, 1, 1, snakeAngle);

        for (BodyPart bodyPart : bodyPartArray) {
            bodyPart.draw(batch, snakePositionX, snakePositionY);
        }

        if (appleAvailable) {
            batch.draw(apple, applePositionX, applePositionY, APPLE_WIDTH / 2, APPLE_HEIGHT / 2, APPLE_WIDTH, APPLE_HEIGHT, 1, 1, 0);
        }

        drawScore();
        batch.end();
    }

    private void clearScreen() {
        Gdx.gl.glClearColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, Color.BLACK.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    private void checkForOutOfBounds() {
        if (snakePositionX >= viewport.getWorldWidth()) {
            snakePositionX = 0;
        }
        if (snakePositionX < 0) {
            snakePositionX = (int) viewport.getWorldWidth() - SNAKE_MOVEMENT;
        }
        if (snakePositionY >= viewport.getWorldHeight()) {
            snakePositionY = 0;
        }
        if (snakePositionY < 0) {
            snakePositionY = (int) viewport.getWorldHeight() - SNAKE_MOVEMENT;
        }
    }

    private void moveSnake() {

        snakeXBeforeUpdate = snakePositionX;
        snakeYBeforeUpdate = snakePositionY;

        switch (snakeDirection) {
            case RIGHT:
                snakePositionX += SNAKE_MOVEMENT;
                return;
            case LEFT:
                snakePositionX -= SNAKE_MOVEMENT;
                return;
            case UP:
                snakePositionY += SNAKE_MOVEMENT;
                return;
            case DOWN:
                snakePositionY -= SNAKE_MOVEMENT;
        }
    }

    private void updateBodyPartsPosition() {
        if (bodyPartArray.size > 0) {
            BodyPart bodyPart = bodyPartArray.removeIndex(0);
            bodyPart.updateBodyPosition(snakeXBeforeUpdate, snakeYBeforeUpdate);
            bodyPartArray.add(bodyPart);
        }
    }

    private void queryInput() {
        boolean lPressed = Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean rPressed = Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean uPressed = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean dPressed = Gdx.input.isKeyPressed(Input.Keys.DOWN);
        if (lPressed) {
            updateDirection(LEFT);
        }
        if (rPressed) {
            updateDirection(RIGHT);
        }
        if (uPressed) {
            updateDirection(UP);
        }
        if (dPressed) {
            updateDirection(DOWN);
        }
    }

    private void checkAndPlaceApple() {
        if (!appleAvailable) {
            //keep setting applePositionX and applePositionX until they are not equal to snakeX and snakeY
            //then go out of the loop
            do {
                applePositionX = MathUtils.random((int) (viewport.getWorldWidth() / SNAKE_MOVEMENT) - 1) * SNAKE_MOVEMENT;
                applePositionY = MathUtils.random((int) (viewport.getWorldHeight() / SNAKE_MOVEMENT) - 1) * SNAKE_MOVEMENT;
                appleAvailable = true;
            }
            while (applePositionX == snakePositionX && applePositionY == snakePositionY);
        }
    }

    private void checkAppleCollision() {
        if (appleAvailable && applePositionX == snakePositionX && applePositionY == snakePositionY) {
            BodyPart bodyPart = new BodyPart(snakeBodyTexture);
            bodyPart.updateBodyPosition(snakePositionX, snakePositionY);
            bodyPartArray.insert(0, bodyPart);
            addToScore();
            appleAvailable = false;
        }
    }

    private void drawGrid() {

        batch.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int x = 0; x < viewport.getWorldWidth(); x += GRID_CELL) {
            for (int y = 0; y < viewport.getWorldHeight(); y += GRID_CELL) {
                shapeRenderer.setColor(Color.GREEN);
                shapeRenderer.rect(x, y, GRID_CELL, GRID_CELL);
            }
        }
        shapeRenderer.end();

    }

    private void updateIfNotOppositeDirection(int newSnakeDirection, int oppositeDirection) {
        if (snakeDirection != oppositeDirection) {
            snakeDirection = newSnakeDirection;
            switch (newSnakeDirection) {
                case LEFT:
                    snakeAngle = ANGLE_LEFT;
                    return;
                case RIGHT:
                    snakeAngle = ANGLE_RIGHT;
                    return;
                case UP:
                    snakeAngle = ANGLE_UP;
                    return;
                case DOWN:
                    snakeAngle = ANGLE_DOWN;
            }
        }
    }

    private void updateDirection(int newSnakeDirection) {
        if (!directionSet && snakeDirection != newSnakeDirection) {
            directionSet = true;
            switch (newSnakeDirection) {
                case LEFT: {
                    updateIfNotOppositeDirection(newSnakeDirection, RIGHT);
                }
                break;
                case RIGHT: {
                    updateIfNotOppositeDirection(newSnakeDirection, LEFT);
                }
                break;
                case UP: {
                    updateIfNotOppositeDirection(newSnakeDirection, DOWN);
                }
                break;
                case DOWN: {
                    updateIfNotOppositeDirection(newSnakeDirection, UP);
                }
                break;
            }
        }
    }

    private void checkSnakeBodyCollision() {
        for (BodyPart bodyPart : bodyPartArray) {
            if (bodyPart.positionX == snakePositionX && bodyPart.positionY == snakePositionY) {
                state = STATE.GAME_OVER;
            }
        }
    }

    private void checkForRestart() {
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) doRestart();
    }

    private void doRestart() {
        state = STATE.PLAYING;
        bodyPartArray.clear();
        snakeDirection = RIGHT;
        snakeAngle = ANGLE_RIGHT;
        directionSet = false;
        timer = MOVE_TIME;
        snakePositionX = 0;
        snakePositionY = 0;
        snakeXBeforeUpdate = 0;
        snakeYBeforeUpdate = 0;
        score = 0;
        appleAvailable = false;
    }

    private void addToScore() {
        score += POINTS_PER_APPLE;
    }

    private void drawScore() {
        if (state == STATE.PLAYING) {
            String scoreAsString = Integer.toString(score);

            glyphLayout.setText(bitmapFont, scoreAsString);
            bitmapFont.draw(batch, scoreAsString, (viewport.getWorldWidth() - glyphLayout.width * 1.25f),
                    (viewport.getWorldHeight() - glyphLayout.height * 18));
        }
    }

}
