package frc.robot;

import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.StartEndCommand;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Constants.DrivetrainConstants;
import frc.robot.Constants.OIConstants;
import frc.robot.commands.DefaultDriveCommand;
import frc.robot.subsystems.Arm;
import frc.robot.subsystems.Drivetrain;
import frc.robot.subsystems.Gripper;
import frc.robot.subsystems.Intake;

public class RobotContainer {
  private final Drivetrain m_drivetrainSubsystem = new Drivetrain();
  private final Arm m_arm = new Arm();
  private final Intake m_intake = new Intake();
  private final Gripper m_gripper = new Gripper();
  // private final Conveyor m_conveyor = new Conveyor();
  // private final ColorSensor colorSensor = new ColorSensor();

  public RobotContainer() {

    // Set up the default command for the drivetrain.
    m_drivetrainSubsystem.setDefaultCommand(new DefaultDriveCommand(
        m_drivetrainSubsystem,
        () -> -modifyAxis(OIConstants.DRIVE_GAMEPAD.getRawAxis(1) *
            DrivetrainConstants.MAX_VELOCITY_METERS_PER_SECOND),
        () -> -modifyAxis(OIConstants.DRIVE_GAMEPAD.getRawAxis(0) *
            DrivetrainConstants.MAX_VELOCITY_METERS_PER_SECOND),
        () -> -modifyAxis(
            OIConstants.DRIVE_GAMEPAD.getRawAxis(3) *
                DrivetrainConstants.MAX_ANGULAR_VELOCITY_RADIANS_PER_SECOND)));

    // Configure the button bindings
    configureButtonBindings();
  }

  /**
   * Use this method to define your button->command mappings. Buttons can be
   * created by
   * instantiating a {@link GenericHID} or one of its subclasses ({@link
   * edu.wpi.first.wpilibj.Joystick} or {@link XboxController}), and then passing
   * it to a {@link
   * edu.wpi.first.wpilibj2.command.button.JoystickButton}.
   */
  private void configureButtonBindings() {
    //Raise/Lower Arm
    new Trigger(OIConstants.DRIVE_GAMEPAD::getYButton).whileTrue(
      new SequentialCommandGroup(
        new InstantCommand(m_arm::highGoal),
        new WaitCommand(1),
        new InstantCommand(m_arm::middleGoal),
        new WaitCommand(1),
        new InstantCommand(m_arm::lowGoal),
        new WaitCommand(1),
        new InstantCommand(m_arm::rest)
    ));

    // Grip Cone
    new Trigger(OIConstants.DRIVE_GAMEPAD::getAButton).whileTrue(
        new StartEndCommand(m_gripper::gripCone, m_gripper::rest, m_gripper));
    // Grip Cube
    new Trigger(OIConstants.DRIVE_GAMEPAD::getBButton).whileTrue(
        new StartEndCommand(m_gripper::gripCube, m_gripper::rest, m_gripper));

    //Actuate intake
    new Trigger(OIConstants.DRIVE_GAMEPAD::getYButton).toggleOnTrue(
      new StartEndCommand(m_intake::actuateIntake, m_intake::actuateIntake, m_intake));

    // Zero gyroscope
    new Trigger(OIConstants.DRIVE_GAMEPAD::getXButton).onTrue(new InstantCommand(m_drivetrainSubsystem::zeroGyroscope));
  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */

  private static double deadband(double value, double deadband) {
    if (Math.abs(value) > deadband) {
      if (value > 0.0) {
        return (value - deadband) / (1.0 - deadband);
      } else {
        return (value + deadband) / (1.0 - deadband);
      }
    } else {
      return 0.0;
    }
  }

  private static double modifyAxis(double value) {
    // Deadband
    value = deadband(value, .1);
    // Cubed the axis, smoother driving
    value = Math.pow(value, 3);

    return value * DrivetrainConstants.SPEED_LIMIT;
  }

  public Command getAutonomousCommand() {
    // return m_drivetrainSubsystem.forward();
    return null;
  }
}
