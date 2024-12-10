// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.noteplayer;

import com.ctre.phoenix6.hardware.CANcoder;
import com.revrobotics.*;


import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.RobotState;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardComponent;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardContainer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants;

public class   ArmModule implements Constants.NotePlayerConstants {

    private final CANSparkMax leftMotor;
    private final CANSparkMax rightMotor;
    private final SparkPIDController leftController;
    private final SparkRelativeEncoder leftEncoder;
    private final SparkPIDController rightController;
    private final SparkRelativeEncoder rightEncoder;

    private final double gravityFF = 0.07;

    private Double setPosition;

    private double percentOut = 0;

    private double pseudoBottomLimit = -10;
    private double pseudoTopLimit = 5;

    private final CANcoder armCANcoder;

    public ArmModule() {
        // Set up the motors:
        leftMotor = new CANSparkMax(ARM_LEFT_MOTOR_ID, CANSparkLowLevel.MotorType.kBrushless);
        rightMotor = new CANSparkMax(ARM_RIGHT_MOTOR_ID, CANSparkLowLevel.MotorType.kBrushless);

        // Set up the PID controllers
        leftController = leftMotor.getPIDController();
        rightController = leftMotor.getPIDController();

        // Set up the encoders
        leftEncoder = (SparkRelativeEncoder) leftMotor.getEncoder();
        rightEncoder = (SparkRelativeEncoder) rightMotor.getEncoder();

        // Set up the CANcoder
        armCANcoder = new CANcoder(ARM_CAN_CODER_ID);
        // Configure the motors
        configureMotors();

        // Reset the motor's integrated encoder based on the CANcoder
        resetMotorEncoderToAbsolute();

        setPosition = leftEncoder.getPosition();
    }

    public double shooterDegreesToMotorRotations(double degrees) {
        //System.out.println("Setting Arm Position: " + (-degrees*motorRotationsPerArmDegree));
        return degrees * ARM_MOTOR_ROTATIONS_PER_SHOOTER_DEGREE;
    }

    public void resetMotorEncoderToAbsolute() {
        double newPosition = getShooterDegrees() * ARM_MOTOR_ROTATIONS_PER_SHOOTER_DEGREE;

        leftEncoder.setPosition(newPosition);
    }

    public void rotateToDegrees(double degrees) {
        setPosition = shooterDegreesToMotorRotations(degrees);
    }

    public void rotateToMotorRotations(double rotations) {
        setPosition = rotations;
    }

    public double getShooterDegrees() {
        return armCANcoder.getAbsolutePosition().getValue() * 360;
    }

    public void rotatePercentOut(double percentOut) {
        this.percentOut = percentOut;
    }

    public void setPseudoLimits(double position) {
        double currentPosition = leftEncoder.getPosition();
        if (currentPosition > position) {
            pseudoTopLimit = currentPosition;
            pseudoBottomLimit = position;
        } else {
            pseudoBottomLimit = currentPosition;
            pseudoTopLimit = position;
        }
        setPosition = position;
    }

    /**
     * @return True if the position of the arm is within {@value ARM_POSITION_ERROR_TOLERANCE} degrees of the setpoint
     */
    public boolean isAtSetPosition() {
        return leftEncoder.getPosition() >= setPosition - 0.75
                && leftEncoder.getPosition() <= setPosition + 0.45;
    }

    private void configureMotors() {

        leftMotor.restoreFactoryDefaults();
        rightMotor.restoreFactoryDefaults();

        // PID Control loop parameters:
        leftController.setP(ArmUpwardsHighGravityPID.p, 0);
        leftController.setI(ArmUpwardsHighGravityPID.i, 0);
        leftController.setD(ArmUpwardsHighGravityPID.d, 0);
        leftController.setIZone(ArmUpwardsHighGravityPID.iZ, 0);
        leftController.setOutputRange(ArmUpwardsHighGravityPID.minOutput, ArmUpwardsHighGravityPID.maxOutput, 0);

        leftController.setP(ArmUpwardsLowGravityPID.p, 1);
        leftController.setI(ArmUpwardsLowGravityPID.i, 1);
        leftController.setD(ArmUpwardsLowGravityPID.d, 1);
        leftController.setIZone(ArmUpwardsLowGravityPID.iZ, 1);
        leftController.setOutputRange(ArmUpwardsLowGravityPID.minOutput, ArmUpwardsLowGravityPID.maxOutput, 1);

        leftController.setP(ArmDownwardsHighGravityPID.p, 2);
        leftController.setI(ArmDownwardsHighGravityPID.i, 2);
        leftController.setD(ArmDownwardsHighGravityPID.d, 2);
        leftController.setIZone(ArmDownwardsHighGravityPID.iZ, 2);
        leftController.setOutputRange(ArmDownwardsHighGravityPID.minOutput, ArmDownwardsHighGravityPID.maxOutput, 2);

        leftController.setP(ArmDownwardsLowGravityPID.p, 3);
        leftController.setI(ArmDownwardsLowGravityPID.i, 3);
        leftController.setD(ArmDownwardsLowGravityPID.d, 3);
        leftController.setIZone(ArmDownwardsLowGravityPID.iZ, 3);
        leftController.setOutputRange(ArmDownwardsLowGravityPID.minOutput, ArmDownwardsLowGravityPID.maxOutput, 3);

        // Voltage Compensation and current limits
        leftMotor.enableVoltageCompensation(12);
        leftMotor.setSmartCurrentLimit(40);
        rightMotor.setSmartCurrentLimit(40);

        // Set soft limits
        leftMotor.setSoftLimit(CANSparkBase.SoftLimitDirection.kForward, ARM_MAX_LIMIT);
        leftMotor.setSoftLimit(CANSparkBase.SoftLimitDirection.kReverse, ARM_MIN_LIMIT);
        leftMotor.enableSoftLimit(CANSparkBase.SoftLimitDirection.kForward, true);
        leftMotor.enableSoftLimit(CANSparkBase.SoftLimitDirection.kReverse, true);

        rightMotor.follow(leftMotor, true);

        // Keep the breaks on so the arm doesn't slam down when disabled
        leftMotor.setIdleMode(CANSparkBase.IdleMode.kBrake);
        rightMotor.setIdleMode(CANSparkBase.IdleMode.kBrake);

        // Save the settings on the controllers, so they stick through power cycles
        leftMotor.burnFlash();
        rightMotor.burnFlash();
    }

    public void periodic() {
        if (setPosition != null) { // TODO: add && !isAtSetPosition()
            // Calculate feed forward based on angle to counteract gravity
            double sineScalar = Math.sin(Math.toRadians(getShooterDegrees() - ARM_BALANCE_DEGREES));
            double feedForward = gravityFF * sineScalar;
            int pidSlot = getPidSlot(sineScalar);

            //System.out.println("Current Position: " + leftEncoder.getPosition() + ", Set Position: " + setPosition + ", Profile slot: " + pidSlot);

            leftController.setReference(setPosition,
                    CANSparkBase.ControlType.kPosition, pidSlot, feedForward, SparkPIDController.ArbFFUnits.kPercentOut);
//            SmartDashboard.putNumber("Arm Motor Position", leftEncoder.getPosition());
//            SmartDashboard.putNumber("Arm Set Position", setPosition);
//            SmartDashboard.putNumber("Arm Profile Slot", pidSlot);
//            SmartDashboard.putNumber("Arm Motor Output", leftMotor.getAppliedOutput());
//            SmartDashboard.putNumber("Arm Motor Current", leftMotor.getOutputCurrent());
//            SmartDashboard.putNumber("Arm Feedforward", feedForward);
//            SmartDashboard.putNumber("Shooter Degrees", getShooterDegrees());
        }

        // Calculate feed forward based on angle to counteract gravity
//        double sineScalar = Math.sin(Math.toRadians(getShooterDegrees() - ARM_BALANCE_DEGREES));
//        double feedForward = gravityFF * sineScalar;
//        if ((percentOut < 0 && leftEncoder.getPosition() > pseudoBottomLimit) || (percentOut > 0 && leftEncoder.getPosition() < pseudoTopLimit)) {
//            leftMotor.set(percentOut + feedForward);
//        } else {
//            leftMotor.set(feedForward);
//        }
        if (RobotState.isDisabled()) {
            resetMotorEncoderToAbsolute();
            setPosition = getShooterDegrees() * ARM_MOTOR_ROTATIONS_PER_SHOOTER_DEGREE;
        }
        //System.out.println("Shooter Angle: " + getShooterDegrees());
//        if (isAtSetPosition()) {
//            System.out.println("Arm at set position!");
//        } else {
//            System.out.println("Arm set position: " + setPosition + ", Actual: " + leftEncoder.getPosition() + ", Arm Degrees: " + getArmDegrees() * motorRotationsPerArmDegree);
//        }
    }

    private int getPidSlot(double sineScalar) {
        int pidSlot;
        double error = setPosition - leftEncoder.getPosition();
        if (error < 0) {
            if (sineScalar < 0) {
                pidSlot = 1;
            }
            else if (sineScalar < 0.7) {
                pidSlot = 3;
            }
            else {
                pidSlot = 2;
            }
        } else {
            if (sineScalar < 0) {
                pidSlot = 3;
            }
            else if (sineScalar < 0.7) {
                pidSlot = 1;
            }
            else {
                pidSlot = 0;
            }
        }
        return pidSlot;
    }
}
