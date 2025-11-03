# AudibleClone Project Migration & Architecture Notes

## Project Overview
This is an **audio streaming application** similar to Audible, with the following key characteristics:

### Core Application Architecture

1. **Content Model**: Pre-loaded lectures only
   - **No user uploads**: Users cannot upload content
   - All lectures are pre-loaded into the system
   - Content is curated and managed by administrators
   - No user-generated content functionality needed

2. **Authentication Provider**: Clerk.com Integration
   - **External Authentication**: Using Clerk.com as the primary authentication provider
   - **Managed by Clerk**: Login, signup, password management, email verification, MFA, social providers
   - **Profile Management**: Clerk handles profile pictures, email management, password resets
   - **Slim User Entity**: Our User entity only needs to store application-specific data

### Database Schema Implications

#### Lectures Table
- **No user_id foreign key needed**: Lectures are pre-loaded content, not user-uploaded
- Remove any user association fields from lectures table
- Focus on content metadata: title, description, audio file paths, categories, etc.

#### Users Table (Slim Design)
Since Clerk.com handles most user data, our User entity should only store:
- `clerk_user_id` (primary link to Clerk)
- Application-specific preferences (playback settings, subscription tier, etc.)
- Usage statistics (listening time, completed lectures)
- Internal role/permission management
- Activity tracking for analytics

**Fields NOT needed** (handled by Clerk):
- email (Clerk manages)
- password (Clerk manages)
- email_verified (Clerk manages)
- two_factor_enabled (Clerk manages)
- profile_image_url (Clerk manages)
- first_name, last_name (Clerk manages)

### Phase Planning Adjustments

#### Phase 1: ✅ Storage & Data Migration
- MinIO setup for audio file storage
- Database migration for lectures (pre-loaded content)

#### Phase 2: 🔄 Authentication & Security (Current - Needs Revision)
- **Revise**: Slim down User entity to only store app-specific data
- **Remove**: User upload/content creation functionality
- **Focus**: Clerk.com JWT integration and role-based access
- **Keep**: User preferences, subscription management, activity tracking

#### Phase 3: Content Management (Admin Only)
- Admin interfaces for managing pre-loaded lectures
- Content categorization and metadata management
- Audio file processing and optimization

#### Phase 4: User Experience
- Audio streaming functionality
- Playback controls and progress tracking
- User preferences and bookmarks
- Search and discovery features

#### Phase 5: Analytics & Recommendations
- User listening analytics
- Content recommendation system
- Usage statistics and reporting

### Technical Decisions Log

1. **No User Uploads**: Simplified content model, no upload endpoints needed
2. **Clerk.com Authentication**: Reduces authentication complexity, slim User entity
3. **Pre-loaded Content**: Content management is admin-only, no user content creation
4. **Audio Streaming Focus**: Application is consumption-focused, not creation-focused

### Database Migration Status

✅ **V1 Migration Complete**: The existing V1__Create_AudibleClone_Schema.sql already implements the correct architecture:
- Users table with clerk_user_id and app-specific fields only
- No user_id foreign key on lectures table (lectures are pre-loaded)
- User activities table for tracking app usage
- Proper indexes and triggers

### Current Status
- **Database Schema**: ✅ Complete and aligned with architecture
- **User Entity**: ✅ Updated to slim design for Clerk integration
- **Next Steps**: Update services and controllers to focus on app-specific functionality

---

*Last Updated: 2025-09-21*
*Project: AudibleClone Backend*
*Phase: 2 - Authentication & Security (Revision Required)*