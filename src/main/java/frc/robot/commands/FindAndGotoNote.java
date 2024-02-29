package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.lib.bluecrew.util.FieldState;
import frc.lib.bluecrew.util.RobotState;
import frc.robot.subsystems.PoseEstimator;
import frc.robot.subsystems.VisionModule;
import frc.robot.subsystems.swervedrive.SwerveDrive;
import org.photonvision.PhotonCamera;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

import java.util.List;


public class FindAndGotoNote extends Command {
    private final SwerveDrive swerveDrive;

    private PhotonCamera notesCamera;

    public FindAndGotoNote(SwerveDrive swerveDrive) {
        this.swerveDrive = swerveDrive;

        // each subsystem used by the command must be passed into the
        // addRequirements() method (which takes a vararg of Subsystem)
        addRequirements(this.swerveDrive);

        notesCamera = VisionModule.getInstance().getNotesIndexerCamera();
    }

    @Override
    public void initialize() {
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        System.out.println("*****\n******\n*******\n******** FINDING AND GOING TO NOTE ********\n*******\n*******\n*****");
    }

    @Override
    public void execute() {

        // Define rotation needed to center-in on note
        double neededRotation = 0.0d; // in degrees
        double neededSpeed = 0.0d; // 1.0 == 100%
        final double heading = swerveDrive.getHeading().getDegrees();

//        System.out.println("... Looking for a note.");

        PhotonPipelineResult pipelineResult = notesCamera.getLatestResult();

        // Check if limelight has found a target
        if (pipelineResult.hasTargets()) {

//            System.out.println("Note found!  ---------------");
            PhotonTrackedTarget target = getBestTarget(pipelineResult);

//            System.out.println("Pitch: " + target.getPitch());
//            System.out.println("Yaw: " + target.getYaw());
//            System.out.println("Heading: " + heading);

            /*
                Observed pitches and yaws from the indexer camera with a note place at the following distances

                Pitches
                =======
                0 ft in front = -23.5 pitch
                1 ft in front = -0.90
                2 ft in front = 9.5
                3 ft in front = 15.0
                4 ft in front = 18.4
                5 ft in front = 21.3

                Yaws
                ======
                0 ft to right = 0 yaw (at 18" distance away from front)
                1 ft to right = 25.0
                2 ft to right = 43.25
                2 ft to right = 24.9 (at 36" distance away from front)

             */

            // Don't go for the note if we're in autonomous, we're getting a center note,
            // and the pitch to the target is greater than we expect
            if (!(RobotState.getInstance().isAutonomous() && target.getPitch() > 17 &&
                    ((PoseEstimator.getInstance().getPose().getX() > 6 && !FieldState.getInstance().onRedAlliance()) ||
                            (PoseEstimator.getInstance().getPose().getX() < 10 && FieldState.getInstance().onRedAlliance())))) {

//                System.out.println("Finding Note");
                // Set that a note is available to pickup
                RobotState.getInstance().setNoteIsAvailable(true);

                // TODO: Remove 1 degree offset once note is being centered / shooter is centered
                neededRotation = target.getYaw()-1;
                double angleOffset = Math.abs(neededRotation);

                if (target.getPitch() < -20 && angleOffset > 10) {
                    // back up if note is well off center and we are close to it
                    neededSpeed = -0.15;
                } else if (target.getPitch() < -15 && angleOffset > 10) {
                    // slow down forward movement if note is well off center and we are close to it
                    neededSpeed = 0.05;
                } else if (target.getPitch() < -27) {
//                System.out.println("Note is at intake.");
                    neededSpeed = 0.02;
                } else if (target.getPitch() < 0) {
//                System.out.println("Note is within a foot");
                    neededSpeed = 0.24;
                    if (angleOffset < 5) {
                        neededSpeed += 0.1;
                    }
                } else if (target.getPitch() < 15) {
//                System.out.println("Note is within 3 feet");
                    neededSpeed = 0.35;
                    if (angleOffset < 15) {
                        neededSpeed += 0.1;
                    }
                } else if (target.getPitch() > 15) {
//                System.out.println("Note is beyond 3 feet");
                    neededSpeed = 0.49;
                    if (angleOffset < 15) {
                        neededSpeed += 0.1;
                    }
                }

/*
            if(target.getYaw() < -angleTolerance) {
                System.out.println("moving left to target!");
//                neededRotation = angleOffset;
            }
            else if(target.getYaw() > angleTolerance) {
                System.out.println("moving right to target!");
//                neededRotation = -angleOffset;
            }
 */
            }

            else {
//                System.out.println("Autonomous Not Getting Note");
            }
        }
        else {
//            System.out.println("No Note in view...");
            neededSpeed = 0.0;
            neededRotation = 0.0;
            // blink the blinkin
            RobotState.getInstance().setNoteIsAvailable(false);
        }

//        System.out.println("Desired speed: " + neededSpeed);
//        System.out.println("Desired rotation (degrees): " + neededRotation);
//        System.out.println("--------------------------------------");

        final double speed = neededSpeed;
        final double rotation = neededRotation;

        // forward/back (translation), left/right (strafe), target heading in degrees
        swerveDrive.driveSwerveDriveAndRotateToAngle(speed, 0.0, heading + rotation);
    }

    @Override
    public boolean isFinished() {

        //        Finish if we're in autonomous and:
        return          (RobotState.getInstance().isAutonomous() && (
                //      There's not a note available
                        !RobotState.getInstance().isNoteIsAvailable() ||
                //      We're on Red Alliance and we're past the center line
                        (FieldState.getInstance().onRedAlliance() && PoseEstimator.getInstance().getPose().getX() < 8.29) ||
                //      or We're on Blue Alliance and we're past the center line
                        (!FieldState.getInstance().onRedAlliance() && PoseEstimator.getInstance().getPose().getX() > 8.29)));
    }

    @Override
    public void end(boolean interrupted) {
        System.out.println("Finished Finding Note");
        RobotState.getInstance().setNoteIsAvailable(false);
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        swerveDrive.resetRotationPIDController();
        swerveDrive.setHoldHeading(swerveDrive.getHeading());
    }

    private PhotonTrackedTarget getBestTarget(PhotonPipelineResult pipelineResult) {

        if(!pipelineResult.hasTargets())
            return null;

        PhotonTrackedTarget bestPitch = null;
        PhotonTrackedTarget bestYaw = null;
        List<PhotonTrackedTarget> targets = pipelineResult.getTargets();
        for(PhotonTrackedTarget t: targets) {
            if(bestPitch == null) {
                bestPitch = t;
                bestYaw = t;
            }
            else {
                if (t.getPitch() < bestPitch.getPitch()) {
                    bestPitch = t;
                }
                if(Math.abs(t.getYaw()) < Math.abs(bestYaw.getYaw())) {
                    bestYaw = t;
                }
            }
        }

        // identify and return the note that has the least yaw, unless another one is considerably closer
        if(Math.abs(bestPitch.getPitch() - bestYaw.getPitch()) > 12)
            return  bestPitch;

        return bestYaw;
    }
}
