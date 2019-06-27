/*
 * Copyright (C) 2019 John Neffenger
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
package org.status6.tofximage;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.stream.IntStream;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Compares the performance of various methods for converting an AWT image to a
 * JavaFX image. Run a quick test with a command like the following:
 * <pre>{@code
 * $HOME/opt/jdk-12.0.1/bin/java \
 *   -Djava.library.path=$HOME/lib/javafx-sdk-13-dev/lib \
 *   -jar target/benchmarks.jar -f 1 -i 1 -wi 1
 * }</pre>
 * <p>
 * Run the benchmarks with their default options for a more thorough test.</p>
 *
 * @author John Neffenger
 */
public class ToFXImage {

    private static final boolean CLEAR_FRAMES = false;
    private static final boolean DEBUG_FRAMES = false;
    private static final boolean NIO_INT_ARGB = false;

    private static void saveImage(String name, WritableImage jfxImage) {
        var reader = jfxImage.getPixelReader();
        int width = (int) jfxImage.getWidth();
        int height = (int) jfxImage.getHeight();
        var awtImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                awtImage.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        try {
            ImageIO.write(awtImage, "png", new File(name.concat(".png")));
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    private static ArrayList<BufferedImage> loadFrames(String filename) {
        ArrayList<BufferedImage> list = new ArrayList<>();
        try (var input = Benchmark.class.getResourceAsStream("/" + filename)) {
            if (input == null) {
                throw new IOException("Error loading image");
            }
            try (var stream = ImageIO.createImageInputStream(input)) {
                ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
                reader.setInput(stream);
                int count = reader.getNumImages(true);
                if (count == 0) {
                    throw new IllegalArgumentException("Error reading GIF image");
                }
                for (int i = 0; i < count; i++) {
                    list.add(reader.read(i));
                }
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
        return list;
    }

    @State(Scope.Thread)
    public static class SourceAwtImage {

        private ArrayList<BufferedImage> frames;
        private int width;
        private int height;
        private int index;

        private void nextFrame() {
            index = index == frames.size() - 1 ? 0 : index + 1;
        }

        @Setup
        public void doSetup() {
            frames = loadFrames("doll-dancing.gif");
            width = frames.get(0).getWidth();
            height = frames.get(0).getHeight();
            index = 0;
        }

        @TearDown
        public void doTearDown() {
            frames.clear();
        }
    }

    @State(Scope.Thread)
    public static class TargetJfxImage {

        private WritableImage image;

        @Setup
        public void doSetup(SourceAwtImage awt) {
            image = new WritableImage(awt.width, awt.height);
        }

        @TearDown
        public void doTearDown() {
        }
    }

    @State(Scope.Thread)
    public static class TargetByteBuffer {

        private ByteBuffer buffer;
        private PixelBuffer<ByteBuffer> pixels;
        private WritableImage image;

        @Setup
        public void doSetup(SourceAwtImage awt) {
            buffer = ByteBuffer.allocateDirect(awt.width * awt.height * Integer.BYTES);
            pixels = new PixelBuffer<>(awt.width, awt.height, buffer, PixelFormat.getByteBgraPreInstance());
            image = new WritableImage(pixels);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        @TearDown
        public void doTearDown() {
        }
    }

    @State(Scope.Thread)
    public static class TargetIntBuffer {

        private IntBuffer buffer;
        private PixelBuffer<IntBuffer> pixels;
        private WritableImage image;

        @Setup
        public void doSetup(SourceAwtImage awt) {
            buffer = IntBuffer.allocate(awt.width * awt.height);
            pixels = new PixelBuffer<>(awt.width, awt.height, buffer, PixelFormat.getIntArgbInstance());
            if (NIO_INT_ARGB) {
                image = new WritableImage(pixels);
            }
        }

        @TearDown
        public void doTearDown() {
        }
    }

    @State(Scope.Thread)
    public static class TempArgbImage {

        private BufferedImage image;
        private Graphics2D graphics;

        @Setup
        public void doSetup(SourceAwtImage awt) {
            image = new BufferedImage(awt.width, awt.height, BufferedImage.TYPE_INT_ARGB);
            graphics = image.createGraphics();
        }

        @TearDown
        public void doTearDown() {
            graphics.dispose();
        }
    }

    @State(Scope.Thread)
    public static class TempArgbPreImage {

        private BufferedImage image;
        private Graphics2D graphics;

        @Setup
        public void doSetup(SourceAwtImage awt) {
            image = new BufferedImage(awt.width, awt.height, BufferedImage.TYPE_INT_ARGB_PRE);
            graphics = image.createGraphics();
        }

        @TearDown
        public void doTearDown() {
            graphics.dispose();
        }
    }

    @State(Scope.Thread)
    public static class TempIntArray {

        private int[] array;

        @Setup
        public void doSetup(SourceAwtImage awt) {
            array = new int[awt.width * awt.height];
        }

        @TearDown
        public void doTearDown() {
        }
    }

    @Benchmark
    public void drawPreSet(SourceAwtImage awt, TempArgbPreImage tmp, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        if (CLEAR_FRAMES) {
            tmp.graphics.clearRect(0, 0, awt.width, awt.height);
        }
        tmp.graphics.drawImage(awtImage, 0, 0, null);
        int[] data = ((DataBufferInt) tmp.image.getRaster().getDataBuffer()).getData();
        jfx.image.getPixelWriter().setPixels(0, 0, awt.width, awt.height,
                PixelFormat.getIntArgbInstance(), data, 0, awt.width);
        if (DEBUG_FRAMES) {
            saveImage("drawPreSet-" + awt.index, jfx.image);
        }
        awt.nextFrame();
    }

    @Benchmark
    public void drawPreSetPre(SourceAwtImage awt, TempArgbPreImage tmp, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        if (CLEAR_FRAMES) {
            tmp.graphics.clearRect(0, 0, awt.width, awt.height);
        }
        tmp.graphics.drawImage(awtImage, 0, 0, null);
        int[] data = ((DataBufferInt) tmp.image.getRaster().getDataBuffer()).getData();
        jfx.image.getPixelWriter().setPixels(0, 0, awt.width, awt.height,
                PixelFormat.getIntArgbPreInstance(), data, 0, awt.width);
        if (DEBUG_FRAMES) {
            saveImage("drawPreSetPre-" + awt.index, jfx.image);
        }
        awt.nextFrame();
    }

    @Benchmark
    public void drawSet(SourceAwtImage awt, TempArgbImage tmp, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        if (CLEAR_FRAMES) {
            tmp.graphics.clearRect(0, 0, awt.width, awt.height);
        }
        tmp.graphics.drawImage(awtImage, 0, 0, null);
        int[] data = ((DataBufferInt) tmp.image.getRaster().getDataBuffer()).getData();
        jfx.image.getPixelWriter().setPixels(0, 0, awt.width, awt.height,
                PixelFormat.getIntArgbInstance(), data, 0, awt.width);
        if (DEBUG_FRAMES) {
            saveImage("drawSet-" + awt.index, jfx.image);
        }
        awt.nextFrame();
    }

    @Benchmark
    public void drawSetPre(SourceAwtImage awt, TempArgbImage tmp, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        if (CLEAR_FRAMES) {
            tmp.graphics.clearRect(0, 0, awt.width, awt.height);
        }
        tmp.graphics.drawImage(awtImage, 0, 0, null);
        int[] data = ((DataBufferInt) tmp.image.getRaster().getDataBuffer()).getData();
        jfx.image.getPixelWriter().setPixels(0, 0, awt.width, awt.height,
                PixelFormat.getIntArgbPreInstance(), data, 0, awt.width);
        if (DEBUG_FRAMES) {
            saveImage("drawSetPre-" + awt.index, jfx.image);
        }
        awt.nextFrame();
    }

    @Benchmark
    public void forLoops(SourceAwtImage awt, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        PixelWriter writer = jfx.image.getPixelWriter();
        for (int y = 0; y < awt.height; y++) {
            for (int x = 0; x < awt.width; x++) {
                writer.setArgb(x, y, awtImage.getRGB(x, y));
            }
        }
        if (DEBUG_FRAMES) {
            saveImage("forLoops-" + awt.index, jfx.image);
        }
        awt.nextFrame();
    }

    @Benchmark
    public void getSet(SourceAwtImage awt, TempIntArray tmp, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        awtImage.getRGB(0, 0, awt.width, awt.height, tmp.array, 0, awt.width);
        jfx.image.getPixelWriter().setPixels(0, 0, awt.width, awt.height,
                PixelFormat.getIntArgbInstance(), tmp.array, 0, awt.width);
        if (DEBUG_FRAMES) {
            saveImage("getSet-" + awt.index, jfx.image);
        }
        awt.nextFrame();
    }

    @Benchmark
    public void getSetPre(SourceAwtImage awt, TempIntArray tmp, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        awtImage.getRGB(0, 0, awt.width, awt.height, tmp.array, 0, awt.width);
        jfx.image.getPixelWriter().setPixels(0, 0, awt.width, awt.height,
                PixelFormat.getIntArgbPreInstance(), tmp.array, 0, awt.width);
        if (DEBUG_FRAMES) {
            saveImage("getSetPre-" + awt.index, jfx.image);
        }
        awt.nextFrame();
    }

    @Benchmark
    public void nioDrawPut(SourceAwtImage awt, TempArgbPreImage tmp, TargetByteBuffer jfx, Blackhole blackhole) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        if (CLEAR_FRAMES) {
            tmp.graphics.clearRect(0, 0, awt.width, awt.height);
        }
        tmp.graphics.drawImage(awtImage, 0, 0, null);
        int[] data = ((DataBufferInt) tmp.image.getRaster().getDataBuffer()).getData();
        jfx.buffer.asIntBuffer().put(data);
        // Simulates Pixelbuffer.updateBuffer on JavaFX Application Thread.
        blackhole.consume(new Rectangle2D(0, 0, awt.width, awt.height));
        if (DEBUG_FRAMES) {
            saveImage("nioDrawPut-" + awt.index, jfx.image);
        }
        awt.nextFrame();
    }

    @Benchmark
    public void nioGetOnly(SourceAwtImage awt, TargetIntBuffer jfx, Blackhole blackhole) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        awtImage.getRGB(0, 0, awt.width, awt.height, jfx.buffer.array(), 0, awt.width);
        // Simulates Pixelbuffer.updateBuffer on JavaFX Application Thread.
        blackhole.consume(new Rectangle2D(0, 0, awt.width, awt.height));
        if (DEBUG_FRAMES && NIO_INT_ARGB) {
            saveImage("nioGetOnly-" + awt.index, jfx.image);
        }
        awt.nextFrame();
    }

    @Benchmark
    public void nioGetPut(SourceAwtImage awt, TempIntArray tmp, TargetByteBuffer jfx, Blackhole blackhole) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        awtImage.getRGB(0, 0, awt.width, awt.height, tmp.array, 0, awt.width);
        jfx.buffer.asIntBuffer().put(tmp.array);
        // Simulates Pixelbuffer.updateBuffer on JavaFX Application Thread.
        blackhole.consume(new Rectangle2D(0, 0, awt.width, awt.height));
        if (DEBUG_FRAMES) {
            saveImage("nioGetPut-" + awt.index, jfx.image);
        }
        awt.nextFrame();
    }

    @Benchmark
    public void streamOrdered(SourceAwtImage awt, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        PixelWriter writer = jfx.image.getPixelWriter();
        IntStream.range(0, awt.width * awt.height).forEachOrdered((i) -> {
            int x = i % awt.width;
            int y = i / awt.width;
            writer.setArgb(x, y, awtImage.getRGB(x, y));
        });
        if (DEBUG_FRAMES) {
            saveImage("streamOrdered-" + awt.index, jfx.image);
        }
        awt.nextFrame();
    }

    @Benchmark
    public void streamParallel(SourceAwtImage awt, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        PixelWriter writer = jfx.image.getPixelWriter();
        IntStream.range(0, awt.width * awt.height).parallel().forEach((i) -> {
            int x = i % awt.width;
            int y = i / awt.width;
            writer.setArgb(x, y, awtImage.getRGB(x, y));
        });
        if (DEBUG_FRAMES) {
            saveImage("streamParallel-" + awt.index, jfx.image);
        }
        awt.nextFrame();
    }

    @Benchmark
    public void swingFXUtils(SourceAwtImage awt, TargetJfxImage jfx, Blackhole blackhole) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        blackhole.consume(SwingFXUtils.toFXImage(awtImage, jfx.image));
        if (DEBUG_FRAMES) {
            saveImage("swingFXUtils-" + awt.index, jfx.image);
        }
        awt.nextFrame();
    }
}
