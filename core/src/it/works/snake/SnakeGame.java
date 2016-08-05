package it.works.snake;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class SnakeGame extends Game {

    @Override
    public void create() {
        setScreen(new GameScreen());
    }

}
