module bedalton.creatures.renderer.cli {
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;
    requires kotlinx.cli;

    requires com.bedalton.coroutines;
    requires com.bedalton.common;
    requires com.bedalton.app;
    requires com.bedalton.log;
    requires com.bedalton.files;
    requires com.bedalton.bytes;


    requires bedalton.creatures.common;
    requires bedalton.creatures.cli;
    requires bedalton.creatures.c2egg;
    requires bedalton.creatures.praydata;
    requires bedalton.creatures.breed.render;
    requires bedalton.creatures.breed.render.support;

    exports bedalton.creatures.breed.render.cli;
}