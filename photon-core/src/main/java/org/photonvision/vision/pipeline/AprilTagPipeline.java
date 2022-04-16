/*
 * Copyright (C) Photon Vision.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.pipeline;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.sound.midi.Track;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.raspi.PicamJNI;
import org.photonvision.vision.apriltag.AprilTagJNI;
import org.photonvision.vision.apriltag.DetectionResult;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.opencv.*;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.*;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.PotentialTarget;
import org.photonvision.vision.target.TrackedTarget;

@SuppressWarnings({"DuplicatedCode"})
public class AprilTagPipeline
        extends CVPipeline<CVPipelineResult, AprilTagPipelineSettings> {
    private final PipelineType pipelineType = PipelineType.AprilTag;           

    private final RotateImagePipe rotateImagePipe = new RotateImagePipe();
    private final GrayscalePipe grayscalePipe = new GrayscalePipe();
    private final AprilTagDetectionPipe aprilTagDetectionPipe = new AprilTagDetectionPipe();
    private final Draw3dTargetsPipe draw3dTargetsPipe = new Draw3dTargetsPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    private final Point[] rectPoints = new Point[4];

    public AprilTagPipeline() {
        settings = new AprilTagPipelineSettings();
    }

    public AprilTagPipeline(AprilTagPipelineSettings settings) {
        this.settings = settings;
    }

    @Override
    protected void setPipeParamsImpl() {
        RotateImagePipe.RotateImageParams rotateImageParams =
                new RotateImagePipe.RotateImageParams(settings.inputImageRotationMode);
        rotateImagePipe.setParams(rotateImageParams);

        if (cameraQuirks.hasQuirk(CameraQuirk.PiCam) && PicamJNI.isSupported()) {
        //     PicamJNI.setThresholds(
        //             settings.hsvHue.getFirst() / 180d,
        //             settings.hsvSaturation.getFirst() / 255d,
        //             settings.hsvValue.getFirst() / 255d,
        //             settings.hsvHue.getSecond() / 180d,
        //             settings.hsvSaturation.getSecond() / 255d,
        //             settings.hsvValue.getSecond() / 255d);

            PicamJNI.setRotation(settings.inputImageRotationMode.value);
            PicamJNI.setShouldCopyColor(settings.inputShouldShow);
        } else {
        //     var hsvParams =
        //             new HSVPipe.HSVParams(
        //                     settings.hsvHue, settings.hsvSaturation, settings.hsvValue, settings.hueInverted);
        //     hsvPipe.setParams(hsvParams);
        }

        // TODO Set the camera to output in grayscale

        AprilTagDetectionPipe.AprilTagDetectionParams aprilTagDetectionParams =
                new AprilTagDetectionPipe.AprilTagDetectionParams(settings.tagFamily);
        aprilTagDetectionPipe.setParams(aprilTagDetectionParams);
        var draw3dTargetsParams =
                new Draw3dTargetsPipe.Draw3dContoursParams(
                        settings.outputShouldDraw,
                        frameStaticProperties.cameraCalibration,
                        settings.targetModel,
                        settings.streamingFrameDivisor);
        draw3dTargetsPipe.setParams(draw3dTargetsParams);
    }

    @Override
    protected CVPipelineResult process(Frame frame, AprilTagPipelineSettings settings) {
        long sumPipeNanosElapsed = 0L;

        CVPipeResult<Mat> grayscalePipeResult;
        Mat rawInputMat;
        if (frame.image.getMat().channels() != 1) {
            var rotateImageResult = rotateImagePipe.run(frame.image.getMat());
            sumPipeNanosElapsed = rotateImageResult.nanosElapsed;

            rawInputMat = frame.image.getMat();

            grayscalePipeResult = grayscalePipe.run(rawInputMat);
            sumPipeNanosElapsed += grayscalePipeResult.nanosElapsed;
        } else {
            // Try to copy the color frame.
            long inputMatPtr = PicamJNI.grabFrame(true);
            if (inputMatPtr != 0) {
                // If we grabbed it (in color copy mode), make a new Mat of it
                rawInputMat = new Mat(inputMatPtr);
            } else {
                //                // Otherwise, use a blank/empty mat as placeholder
                //                rawInputMat = new Mat();
                // Otherwise, the input mat is frame we got from the camera
                rawInputMat = frame.image.getMat();
            }

            // We can skip a few steps if the image is single channel because we've already done them on
            // the GPU
            grayscalePipeResult = new CVPipeResult<>();
            grayscalePipeResult.output = frame.image.getMat();
            grayscalePipeResult.nanosElapsed = MathUtils.wpiNanoTime() - frame.timestampNanos;

            sumPipeNanosElapsed += grayscalePipeResult.nanosElapsed;
        }

        List<TrackedTarget> targetList;
        CVPipeResult<List<DetectionResult>> tagDetectionPipeResult;
        
        tagDetectionPipeResult = aprilTagDetectionPipe.run(grayscalePipeResult.output);
        sumPipeNanosElapsed += tagDetectionPipeResult.nanosElapsed;

        targetList = List.of();

        for (DetectionResult detection : tagDetectionPipeResult.output) {
                // populate the target list
                // Challenge here is that TrackedTarget functions with OpenCV Contours
                System.out.println(detection.getId());
                
        }

        draw3dTargetsPipe.run(Pair.of(rawInputMat, targetList));



        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CVPipelineResult(
                sumPipeNanosElapsed,
                fps,
                targetList,
                new Frame(new CVMat(grayscalePipeResult.output), frame.frameStaticProperties),
                new Frame(new CVMat(rawInputMat), frame.frameStaticProperties));
    }
}