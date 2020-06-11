/*
 * Copyright (C) 2019-2020 John Neffenger
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

import java.awt.Color;
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
 * $HOME/opt/jdk-14.0.1/bin/java \
 *   -Djava.library.path=$HOME/lib/javafx-sdk-15/lib \
 *   -jar target/benchmarks.jar -f 1 -i 1 -wi 1
 * }</pre>
 * <p>
 * Run the benchmarks with their default options for a more thorough test.</p>
 *
 * @author John Neffenger
 */
public class Benchmarks {

    /**
     * The file name of the source AWT image.
     */
    private static final String FILE_NAME = "doll-dancing.gif";

    /**
     * Whether to clear the background of the intermediate AWT image for each
     * frame. Set to {@code true} if the source AWT image contains transparency.
     */
    private static final boolean CLEAR_FRAMES = false;

    /**
     * Whether to save each converted image to a file in PNG format.
     */
    private static final boolean DEBUG_FRAMES = false;

    /**
     * Clears the background of the intermediate AWT image to be fully
     * transparent. Call this method before drawing an image with transparency
     * into an intermediate AWT image.
     *
     * @param graphics the graphics object of the intermediate AWT image
     * @param width the image width in pixels
     * @param height the image height in pixels
     */
    private static void clearRect(Graphics2D graphics, int width, int height) {
        if (CLEAR_FRAMES) {
            graphics.setBackground(new Color(0, true));
            graphics.clearRect(0, 0, width, height);
        }
    }

    /**
     * Saves the JavaFX image in PNG format with the given file name.
     *
     * @param name the file name
     * @param jfxImage the JavaFX image
     */
    private static void saveImage(String name, WritableImage jfxImage) {
        if (DEBUG_FRAMES) {
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
    }

    /**
     * Loads all frames of a GIF image file.
     *
     * @param filename the file name of the GIF image
     * @return a list of frames as AWT images
     */
    private static ArrayList<BufferedImage> loadFrames(String filename) {
        ArrayList<BufferedImage> list = new ArrayList<>();
        try ( var input = Benchmark.class.getResourceAsStream("/" + filename)) {
            if (input == null) {
                throw new IOException("Error loading image");
            }
            try ( var stream = ImageIO.createImageInputStream(input)) {
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
            frames = loadFrames(FILE_NAME);
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
        private PixelFormat<ByteBuffer> format;
        private PixelBuffer<ByteBuffer> pixels;
        private WritableImage image;

        @Setup
        public void doSetup(SourceAwtImage awt) {
            // Creates a PixelBuffer using BYTE_BGRA_PRE pixel format.
            buffer = ByteBuffer.allocateDirect(awt.width * awt.height * Integer.BYTES);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            format = PixelFormat.getByteBgraPreInstance();
            pixels = new PixelBuffer<>(awt.width, awt.height, buffer, format);
            image = new WritableImage(pixels);
        }

        @TearDown
        public void doTearDown() {
        }
    }

    @State(Scope.Thread)
    public static class TargetIntBuffer {

        private IntBuffer buffer;
        private PixelFormat<IntBuffer> format;
        private PixelBuffer<IntBuffer> pixels;
        private WritableImage image;

        @Setup
        public void doSetup(SourceAwtImage awt) {
            // Creates a PixelBuffer using INT_ARGB_PRE pixel format.
            buffer = IntBuffer.allocate(awt.width * awt.height);
            format = PixelFormat.getIntArgbPreInstance();
            pixels = new PixelBuffer<>(awt.width, awt.height, buffer, format);
            image = new WritableImage(pixels);
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
    public static class TempArray {

        private int[] array;

        @Setup
        public void doSetup(SourceAwtImage awt) {
            array = new int[awt.width * awt.height];
        }

        @TearDown
        public void doTearDown() {
        }
    }

    /**
     * Converts pixel by pixel using a sequential stream. This method copies
     * pixels in the INT_ARGB format as INT_ARGB pixels (correct).
     *
     * @param awt the source AWT image
     * @param jfx the target JavaFX image
     */
    @Benchmark
    public void forEachOrdered(SourceAwtImage awt, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        PixelWriter writer = jfx.image.getPixelWriter();
        IntStream.range(0, awt.width * awt.height).forEachOrdered((i) -> {
            int x = i % awt.width;
            int y = i / awt.width;
            writer.setArgb(x, y, awtImage.getRGB(x, y));
        });
        saveImage("forEachOrdered-" + awt.index, jfx.image);
        awt.nextFrame();
    }

    /**
     * Converts pixel by pixel using a parallel stream. This method copies
     * pixels in the INT_ARGB format as INT_ARGB pixels (correct).
     *
     * @param awt the source AWT image
     * @param jfx the target JavaFX image
     */
    @Benchmark
    public void forEachParallel(SourceAwtImage awt, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        PixelWriter writer = jfx.image.getPixelWriter();
        IntStream.range(0, awt.width * awt.height).parallel().forEach((i) -> {
            int x = i % awt.width;
            int y = i / awt.width;
            writer.setArgb(x, y, awtImage.getRGB(x, y));
        });
        saveImage("forEachParallel-" + awt.index, jfx.image);
        awt.nextFrame();
    }

    /**
     * Converts pixel by pixel using nested <i>for loops</i>. This method copies
     * pixels in the INT_ARGB format as INT_ARGB pixels (correct).
     *
     * @param awt the source AWT image
     * @param jfx the target JavaFX image
     */
    @Benchmark
    public void forLoopsNested(SourceAwtImage awt, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        PixelWriter writer = jfx.image.getPixelWriter();
        for (int y = 0; y < awt.height; y++) {
            for (int x = 0; x < awt.width; x++) {
                writer.setArgb(x, y, awtImage.getRGB(x, y));
            }
        }
        saveImage("forLoopsNested" + awt.index, jfx.image);
        awt.nextFrame();
    }

    /**
     * Draws the source AWT image into an intermediate AWT image; then gets the
     * intermediate raster data and puts it into the byte buffer of a
     * {@code PixelBuffer}. This method copies pixels in the INT_ARGB_PRE format
     * into a byte buffer with little-endian byte order as BYTE_BGRA_PRE pixels
     * (correct).
     *
     * @param awt the source AWT image
     * @param tmp the intermediate AWT image in INT_ARGB_PRE pixel format
     * @param jfx the target JavaFX image backed by a
     * {@code PixelBuffer<ByteBuffer>} in BYTE_BGRA_PRE pixel format
     * @param blackhole used to simulate a call to
     * {@code PixelBuffer.updateBuffer}
     */
    @Benchmark
    public void putArgbPreIntoBytes(SourceAwtImage awt, TempArgbPreImage tmp, TargetByteBuffer jfx, Blackhole blackhole) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        clearRect(tmp.graphics, awt.width, awt.height);
        tmp.graphics.drawImage(awtImage, 0, 0, null);
        int[] data = ((DataBufferInt) tmp.image.getRaster().getDataBuffer()).getData();
        jfx.buffer.asIntBuffer().put(data);
        // Simulates PixelBuffer.updateBuffer on JavaFX Application Thread
        blackhole.consume(new Rectangle2D(0, 0, awt.width, awt.height));
        saveImage("putArgbPreIntoBytes-" + awt.index, jfx.image);
        awt.nextFrame();
    }

    /**
     * Draws the source AWT image into an intermediate AWT image; then gets the
     * intermediate raster data and puts it into the integer buffer of a
     * {@code PixelBuffer}. This method copies pixels in the INT_ARGB_PRE format
     * into an integer buffer as INT_ARGB_PRE pixels (correct).
     *
     * @param awt the source AWT image
     * @param tmp the intermediate AWT image in INT_ARGB_PRE pixel format
     * @param jfx the target JavaFX image backed by a
     * {@code PixelBuffer<IntBuffer>} in INT_ARGB_PRE pixel format
     * @param blackhole used to simulate a call to
     * {@code PixelBuffer.updateBuffer}
     */
    @Benchmark
    public void putArgbPreIntoInts(SourceAwtImage awt, TempArgbPreImage tmp, TargetIntBuffer jfx, Blackhole blackhole) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        clearRect(tmp.graphics, awt.width, awt.height);
        tmp.graphics.drawImage(awtImage, 0, 0, null);
        int[] data = ((DataBufferInt) tmp.image.getRaster().getDataBuffer()).getData();
        jfx.buffer.put(data);
        // Simulates PixelBuffer.updateBuffer on JavaFX Application Thread
        blackhole.consume(new Rectangle2D(0, 0, awt.width, awt.height));
        saveImage("putArgbPreIntoInts-" + awt.index, jfx.image);
        jfx.buffer.clear();
        awt.nextFrame();
    }

    /**
     * Gets the pixels of the source AWT image into an intermediate array; then
     * puts the pixels into the byte buffer of a {@code PixelBuffer}. This
     * method copies pixels in the INT_ARGB format into a byte buffer with
     * little-endian byte order as BYTE_BGRA_PRE pixels (wrong alpha).
     *
     * @param awt the source AWT image
     * @param tmp the intermediate array in INT_ARGB pixel format
     * @param jfx the target JavaFX image backed by a
     * {@code PixelBuffer<ByteBuffer>} in BYTE_BGRA_PRE pixel format
     * @param blackhole used to simulate a call to
     * {@code PixelBuffer.updateBuffer}
     */
    @Benchmark
    public void putArrayIntoBytes(SourceAwtImage awt, TempArray tmp, TargetByteBuffer jfx, Blackhole blackhole) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        awtImage.getRGB(0, 0, awt.width, awt.height, tmp.array, 0, awt.width);
        jfx.buffer.asIntBuffer().put(tmp.array);
        // Simulates PixelBuffer.updateBuffer on JavaFX Application Thread
        blackhole.consume(new Rectangle2D(0, 0, awt.width, awt.height));
        saveImage("putArrayIntoBytes-" + awt.index, jfx.image);
        awt.nextFrame();
    }

    /**
     * Gets the pixels of the source AWT image directly into the integer buffer
     * of a {@code PixelBuffer}. This method copies pixels in the INT_ARGB
     * format into an integer buffer as INT_ARGB_PRE pixels (wrong alpha).
     *
     * @param awt the source AWT image
     * @param jfx the target JavaFX image backed by a
     * {@code PixelBuffer<IntBuffer>} in INT_ARGB_PRE pixel format
     * @param blackhole used to simulate a call to
     * {@code PixelBuffer.updateBuffer}
     */
    @Benchmark
    public void putDirectIntoInts(SourceAwtImage awt, TargetIntBuffer jfx, Blackhole blackhole) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        awtImage.getRGB(0, 0, awt.width, awt.height, jfx.buffer.array(), 0, awt.width);
        // Simulates PixelBuffer.updateBuffer on JavaFX Application Thread
        blackhole.consume(new Rectangle2D(0, 0, awt.width, awt.height));
        saveImage("putDirectIntoInts-" + awt.index, jfx.image);
        awt.nextFrame();
    }

    /**
     * Draws the source AWT image into an intermediate AWT image; then gets the
     * intermediate raster data and writes it to the JavaFX image using a
     * {@code PixelWriter}. This method copies pixels in the INT_ARGB format as
     * INT_ARGB pixels (correct).
     *
     * @param awt the source AWT image
     * @param tmp the intermediate AWT image in INT_ARGB pixel format
     * @param jfx the target JavaFX image
     */
    @Benchmark
    public void setArgbAsArgb(SourceAwtImage awt, TempArgbImage tmp, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        clearRect(tmp.graphics, awt.width, awt.height);
        tmp.graphics.drawImage(awtImage, 0, 0, null);
        int[] data = ((DataBufferInt) tmp.image.getRaster().getDataBuffer()).getData();
        jfx.image.getPixelWriter().setPixels(0, 0, awt.width, awt.height,
                PixelFormat.getIntArgbInstance(), data, 0, awt.width);
        saveImage("setArgbAsArgb-" + awt.index, jfx.image);
        awt.nextFrame();
    }

    /**
     * Draws the source AWT image into an intermediate AWT image; then gets the
     * intermediate raster data and writes it to the JavaFX image using a
     * {@code PixelWriter}. This method copies pixels in the INT_ARGB format as
     * INT_ARGB_PRE pixels (wrong alpha).
     *
     * @param awt the source AWT image
     * @param tmp the intermediate AWT image in INT_ARGB pixel format
     * @param jfx the target JavaFX image
     */
    @Benchmark
    public void setArgbAsArgbPre(SourceAwtImage awt, TempArgbImage tmp, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        clearRect(tmp.graphics, awt.width, awt.height);
        tmp.graphics.drawImage(awtImage, 0, 0, null);
        int[] data = ((DataBufferInt) tmp.image.getRaster().getDataBuffer()).getData();
        jfx.image.getPixelWriter().setPixels(0, 0, awt.width, awt.height,
                PixelFormat.getIntArgbPreInstance(), data, 0, awt.width);
        saveImage("setArgbAsArgbPre-" + awt.index, jfx.image);
        awt.nextFrame();
    }

    /**
     * Draws the source AWT image into an intermediate AWT image; then gets the
     * intermediate raster data and writes it to the JavaFX image using a
     * {@code PixelWriter}. This method copies pixels in the INT_ARGB_PRE format
     * as INT_ARGB pixels (wrong alpha).
     *
     * @param awt the source AWT image
     * @param tmp the intermediate AWT image in INT_ARGB_PRE pixel format
     * @param jfx the target JavaFX image
     */
    @Benchmark
    public void setArgbPreAsArgb(SourceAwtImage awt, TempArgbPreImage tmp, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        clearRect(tmp.graphics, awt.width, awt.height);
        tmp.graphics.drawImage(awtImage, 0, 0, null);
        int[] data = ((DataBufferInt) tmp.image.getRaster().getDataBuffer()).getData();
        jfx.image.getPixelWriter().setPixels(0, 0, awt.width, awt.height,
                PixelFormat.getIntArgbInstance(), data, 0, awt.width);
        saveImage("setArgbPreAsArgb-" + awt.index, jfx.image);
        awt.nextFrame();
    }

    /**
     * Draws the source AWT image into an intermediate AWT image; then gets the
     * intermediate raster data and writes it to the JavaFX image using a
     * {@code PixelWriter}. This method copies pixels in the INT_ARGB_PRE format
     * as INT_ARGB_PRE pixels (correct).
     *
     * @param awt the source AWT image
     * @param tmp the intermediate AWT image in INT_ARGB_PRE pixel format
     * @param jfx the target JavaFX image
     */
    @Benchmark
    public void setArgbPreAsArgbPre(SourceAwtImage awt, TempArgbPreImage tmp, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        clearRect(tmp.graphics, awt.width, awt.height);
        tmp.graphics.drawImage(awtImage, 0, 0, null);
        int[] data = ((DataBufferInt) tmp.image.getRaster().getDataBuffer()).getData();
        jfx.image.getPixelWriter().setPixels(0, 0, awt.width, awt.height,
                PixelFormat.getIntArgbPreInstance(), data, 0, awt.width);
        saveImage("setArgbPreAsArgbPre-" + awt.index, jfx.image);
        awt.nextFrame();
    }

    /**
     * Gets the pixels of the source AWT image into an intermediate array; then
     * writes the array to the JavaFX image using a {@code PixelWriter}. This
     * method copies pixels in the INT_ARGB format as INT_ARGB pixels (correct).
     *
     * @param awt the source AWT image
     * @param tmp the intermediate array in INT_ARGB pixel format
     * @param jfx the target JavaFX image
     */
    @Benchmark
    public void setArrayAsArgb(SourceAwtImage awt, TempArray tmp, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        awtImage.getRGB(0, 0, awt.width, awt.height, tmp.array, 0, awt.width);
        jfx.image.getPixelWriter().setPixels(0, 0, awt.width, awt.height,
                PixelFormat.getIntArgbInstance(), tmp.array, 0, awt.width);
        saveImage("setArrayAsArgb-" + awt.index, jfx.image);
        awt.nextFrame();
    }

    /**
     * Gets the pixels of the source AWT image into an intermediate array; then
     * writes the array to the JavaFX image using a {@code PixelWriter}. This
     * method copies pixels in the INT_ARGB format as INT_ARGB_PRE pixels (wrong
     * alpha).
     *
     * @param awt the source AWT image
     * @param tmp the intermediate array in INT_ARGB pixel format
     * @param jfx the target JavaFX image
     */
    @Benchmark
    public void setArrayAsArgbPre(SourceAwtImage awt, TempArray tmp, TargetJfxImage jfx) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        awtImage.getRGB(0, 0, awt.width, awt.height, tmp.array, 0, awt.width);
        jfx.image.getPixelWriter().setPixels(0, 0, awt.width, awt.height,
                PixelFormat.getIntArgbPreInstance(), tmp.array, 0, awt.width);
        saveImage("setArrayAsArgbPre-" + awt.index, jfx.image);
        awt.nextFrame();
    }

    /**
     * Converts the AWT image into a JavaFX image using the public JavaFX
     * utility method {@link SwingFXUtils#toFXImage}. This method creates a
     * JavaFX image with a pixel format of either INT_ARGB or INT_ARGB_PRE,
     * depending on the source AWT image (correct).
     *
     * @param awt the source AWT image
     * @param jfx the target JavaFX image
     * @param blackhole used to consume the output of the utility method
     */
    @Benchmark
    public void toFXImage(SourceAwtImage awt, TargetJfxImage jfx, Blackhole blackhole) {
        BufferedImage awtImage = awt.frames.get(awt.index);
        blackhole.consume(SwingFXUtils.toFXImage(awtImage, jfx.image));
        saveImage("toFXImage-" + awt.index, jfx.image);
        awt.nextFrame();
    }
}
