module com.bedalton.creatures.renderer.cli {
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;
    requires kotlinx.cli.jvm;

    requires com.bedalton.coroutines;
    requires com.bedalton.common;
    requires com.bedalton.app;
    requires com.bedalton.log;
    requires com.bedalton.files;
    requires com.bedalton.bytes;


    requires com.bedalton.creatures.common;
    requires com.bedalton.creatures.cli;
    requires com.bedalton.creatures.c2egg;
    requires com.bedalton.creatures.praydata;
    requires com.bedalton.creatures.breed.render;
    requires com.bedalton.creatures.breed.render.support;
    requires com.bedalton.creatures.exports.minimal;

    exports com.bedalton.creatures.breed.render.cli;
}