package it.works.snake;


import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;

public class BodyPart {

    int positionX = 0;
    int positionY = 0;

    private Texture texture;

    public BodyPart(Texture texture) {
        this.texture = texture;
    }

    public void updateBodyPosition(int x, int y) {
        this.positionX = x;
        this.positionY = y;
    }

    public void draw(Batch batch, int snakeX, int snakeY) {
        if (!(positionX == snakeX && positionY == snakeY)) {
            batch.draw(texture, positionX, positionY);
        }
    }

}
