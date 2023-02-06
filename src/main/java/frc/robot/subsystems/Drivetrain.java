package frc.robot.subsystems;

import com.kauailabs.navx.frc.AHRS;
import com.pathplanner.lib.PathConstraints;
import com.pathplanner.lib.PathPlanner;
import com.swervedrivespecialties.swervelib.Mk3SwerveModuleHelper;
import com.swervedrivespecialties.swervelib.SwerveModule;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.I2C.Port;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.DrivetrainConstants;

public class Drivetrain extends SubsystemBase {
        private final SwerveModule m_frontLeftModule;
        private final SwerveModule m_frontRightModule;
        private final SwerveModule m_backLeftModule;
        private final SwerveModule m_backRightModule;
        private SwerveDriveOdometry odometry;
        private SwerveModulePosition[] modulePosition = new SwerveModulePosition[4];
        SwerveModule[] module = new SwerveModule[4];
        private final AHRS m_navx = new AHRS(Port.kMXP);

        // Creates our swerve kinematics using the robots track width and wheel base
        private final SwerveDriveKinematics m_kinematics = new SwerveDriveKinematics(
                        // Front left
                        new Translation2d(DrivetrainConstants.DRIVETRAIN_TRACKWIDTH_METERS / 2.0,
                                        DrivetrainConstants.DRIVETRAIN_WHEELBASE_METERS / 2.0),
                        // Front right
                        new Translation2d(DrivetrainConstants.DRIVETRAIN_TRACKWIDTH_METERS / 2.0,
                                        -DrivetrainConstants.DRIVETRAIN_WHEELBASE_METERS / 2.0),
                        // Back left
                        new Translation2d(-DrivetrainConstants.DRIVETRAIN_TRACKWIDTH_METERS / 2.0,
                                        DrivetrainConstants.DRIVETRAIN_WHEELBASE_METERS / 2.0),
                        // Back right
                        new Translation2d(-DrivetrainConstants.DRIVETRAIN_TRACKWIDTH_METERS / 2.0,
                                        -DrivetrainConstants.DRIVETRAIN_WHEELBASE_METERS / 2.0));

        private ChassisSpeeds m_chassisSpeeds = new ChassisSpeeds(0.0, 0.0, 0.0);

        public Drivetrain() {
                ShuffleboardTab tab = Shuffleboard.getTab("Drivetrain");
                // The module has two NEOs on it. One for steering and one for driving.
                m_frontLeftModule = Mk3SwerveModuleHelper.createNeo(
                                tab.getLayout("Front Left Module", BuiltInLayouts.kList)
                                                .withSize(2, 4)
                                                .withPosition(0, 0),
                                Mk3SwerveModuleHelper.GearRatio.STANDARD,
                                DrivetrainConstants.FRONT_LEFT_MODULE_DRIVE_MOTOR,
                                DrivetrainConstants.FRONT_LEFT_MODULE_STEER_MOTOR,
                                DrivetrainConstants.FRONT_LEFT_MODULE_STEER_ENCODER,
                                DrivetrainConstants.FRONT_LEFT_MODULE_STEER_OFFSET);
                m_frontRightModule = Mk3SwerveModuleHelper.createNeo(
                                tab.getLayout("Front Right Module", BuiltInLayouts.kList)
                                                .withSize(2, 4)
                                                .withPosition(2, 0),
                                Mk3SwerveModuleHelper.GearRatio.STANDARD,
                                DrivetrainConstants.FRONT_RIGHT_MODULE_DRIVE_MOTOR,
                                DrivetrainConstants.FRONT_RIGHT_MODULE_STEER_MOTOR,
                                DrivetrainConstants.FRONT_RIGHT_MODULE_STEER_ENCODER,
                                DrivetrainConstants.FRONT_RIGHT_MODULE_STEER_OFFSET);
                m_backLeftModule = Mk3SwerveModuleHelper.createNeo(
                                tab.getLayout("Back Left Module", BuiltInLayouts.kList)
                                                .withSize(2, 4)
                                                .withPosition(4, 0),
                                Mk3SwerveModuleHelper.GearRatio.STANDARD,
                                DrivetrainConstants.BACK_LEFT_MODULE_DRIVE_MOTOR,
                                DrivetrainConstants.BACK_LEFT_MODULE_STEER_MOTOR,
                                DrivetrainConstants.BACK_LEFT_MODULE_STEER_ENCODER,
                                DrivetrainConstants.BACK_LEFT_MODULE_STEER_OFFSET);
                m_backRightModule = Mk3SwerveModuleHelper.createNeo(
                                tab.getLayout("Back Right Module", BuiltInLayouts.kList)
                                                .withSize(2, 4)
                                                .withPosition(6, 0),
                                Mk3SwerveModuleHelper.GearRatio.STANDARD,
                                DrivetrainConstants.BACK_RIGHT_MODULE_DRIVE_MOTOR,
                                DrivetrainConstants.BACK_RIGHT_MODULE_STEER_MOTOR,
                                DrivetrainConstants.BACK_RIGHT_MODULE_STEER_ENCODER,
                                DrivetrainConstants.BACK_RIGHT_MODULE_STEER_OFFSET);

                // Adding modules to array so we can get positions easier
                module[0] = m_frontLeftModule;
                module[1] = m_frontRightModule;
                module[2] = m_backLeftModule;
                module[3] = m_backRightModule;

                zeroGyroscope();
                zeroPositions();

                odometry = new SwerveDriveOdometry(m_kinematics,
                                Rotation2d.fromDegrees(m_navx.getFusedHeading()), modulePosition);
        }

        // Positions
        // Get distance the bot has traveled in meters relative to the starting position
        public SwerveModulePosition getPosition(int moduleNumber) {
                return new SwerveModulePosition(
                                (module[moduleNumber].getDriveEncoder().getPosition() *
                                                (DrivetrainConstants.WHEEL_DIAMETER
                                                                * Math.PI / (DrivetrainConstants.GEAR_RATIO * 2048.0))),
                                getGyroscopeRotation());
        }

        public void zeroPositions() {
                modulePosition[0] = new SwerveModulePosition();
                modulePosition[1] = new SwerveModulePosition();
                modulePosition[2] = new SwerveModulePosition();
                modulePosition[3] = new SwerveModulePosition();
        }

        // Gyroscope
        public void zeroGyroscope() {
                m_navx.zeroYaw();
        }

        public Rotation2d getGyroscopeRotation() {
                return m_navx.getRotation2d();
        }

        // Pose
        public void ResetPose(Pose2d pos) {
                odometry.resetPosition(getGyroscopeRotation(), modulePosition, getPose());
        }

        private void updatePose() {
                odometry.update(getGyroscopeRotation(), modulePosition);
        }

        public Pose2d getPose() {

                Pose2d tmp = odometry.getPoseMeters();

                return tmp;
        }

        // Literal Speeds
        private void setSpeeds(ChassisSpeeds chassisSpeeds) {
                SwerveModuleState[] states = m_kinematics.toSwerveModuleStates(chassisSpeeds);
                SwerveDriveKinematics.desaturateWheelSpeeds(states, DrivetrainConstants.MAX_VELOCITY_METERS_PER_SECOND);

                // Update virtual states
                setStates(states);
        }

        // Virtual Speeds
        public void setStates(SwerveModuleState[] state) {
                updatePose();
                m_frontLeftModule.set(
                                (state[0].speedMetersPerSecond / DrivetrainConstants.MAX_VELOCITY_METERS_PER_SECOND
                                                * DrivetrainConstants.MAX_VOLTAGE)
                                                * DrivetrainConstants.SPEED_LIMIT,
                                state[0].angle.getRadians());
                m_frontRightModule.set(
                                (state[1].speedMetersPerSecond / DrivetrainConstants.MAX_VELOCITY_METERS_PER_SECOND
                                                * DrivetrainConstants.MAX_VOLTAGE)
                                                * DrivetrainConstants.SPEED_LIMIT,
                                state[1].angle.getRadians());
                m_backLeftModule.set(
                                (state[2].speedMetersPerSecond / DrivetrainConstants.MAX_VELOCITY_METERS_PER_SECOND
                                                * DrivetrainConstants.MAX_VOLTAGE)
                                                * DrivetrainConstants.SPEED_LIMIT,
                                state[2].angle.getRadians());
                m_backRightModule.set(
                                (state[3].speedMetersPerSecond / DrivetrainConstants.MAX_VELOCITY_METERS_PER_SECOND
                                                * DrivetrainConstants.MAX_VOLTAGE)
                                                * DrivetrainConstants.SPEED_LIMIT,
                                state[3].angle.getRadians());

                modulePosition[0] = getPosition(0);
                modulePosition[1] = getPosition(1);
                modulePosition[2] = getPosition(2);
                modulePosition[3] = getPosition(3);
        }

        public SwerveDriveKinematics getKinematics() {
                return this.m_kinematics;
        }

        public void drive(ChassisSpeeds chassisSpeeds) {
                m_chassisSpeeds = chassisSpeeds;
        }

        @Override
        public void periodic() {

                setSpeeds(m_chassisSpeeds);
        }

        public Command forward() {
                return DrivetrainConstants.m_AUTO_BUILDER.fullAuto(PathPlanner.loadPathGroup("Forward",
                                new PathConstraints(4, 3)));
        }
}