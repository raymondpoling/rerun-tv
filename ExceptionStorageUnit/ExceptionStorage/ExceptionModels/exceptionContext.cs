using System;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata;
using System.Configuration;

namespace ExceptionStorage.ExceptionModels
{
    public partial class exceptionContext : DbContext
    {
        public exceptionContext()
        {
        }

        public exceptionContext(DbContextOptions<exceptionContext> options)
            : base(options)
        {
        }

        public virtual DbSet<Results> Results { get; set; }
        public virtual DbSet<Tests> Tests { get; set; }

        protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
        {
            try {
                var s =  Microsoft
   .Extensions
   .Configuration
   .ConfigurationExtensions
   .GetConnectionString(this.Configuration, "MySql");
                foreach (object i in s) {
                    Console.WriteLine("Connection string: " + i.ToString());
                }
                
            } catch (Exception e) {
                Console.WriteLine("Failed?" + e.ToString());
            }
            
            if (!optionsBuilder.IsConfigured)
            {
                    optionsBuilder.UseMySQL(ConfigurationManager.ConnectionStrings["MySql"].ConnectionString);
            }
        }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            modelBuilder.Entity<Results>(entity =>
            {
                entity.HasKey("Id");

                entity.ToTable("results");

                entity.HasIndex(e => e.Id)
                    .HasName("id")
                    .IsUnique();

                entity.HasIndex(e => e.TestId)
                    .HasName("test_id");

                entity.HasIndex(e => new { e.Date, e.TestId })
                    .HasName("by_date_id");

                entity.Property(e => e.Date).HasColumnName("date");

                entity.Property(e => e.Id)
                    .HasColumnName("id")
                    .HasColumnType("bigint(20) unsigned")
                    .ValueGeneratedOnAdd();

                entity.Property(e => e.PassFail)
                    .HasColumnName("passFail")
                    .HasColumnType("tinyint(1)");

                entity.Property(e => e.RemediationSucceeded)
                    .HasColumnName("remediationSucceeded")
                    .HasColumnType("tinyint(1)");

                entity.Property(e => e.StatusMessage).HasColumnName("statusMessage");

                entity.Property(e => e.TestId)
                    .HasColumnName("test_id")
                    .HasColumnType("bigint(20) unsigned");

                entity.HasOne(d => d.Test)
                    .WithMany()
                    .HasPrincipalKey(p => p.Id)
                    .HasForeignKey(d => d.TestId)
                    .HasConstraintName("results_ibfk_1");
            });

            modelBuilder.Entity<Tests>(entity =>
            {
                entity.HasKey("Id");

                entity.ToTable("tests");

                entity.HasIndex(e => e.Id)
                    .HasName("id")
                    .IsUnique();

                entity.HasIndex(e => e.Name)
                    .HasName("by_name")
                    .IsUnique();

                entity.Property(e => e.Cron)
                    .HasColumnName("cron")
                    .HasMaxLength(14)
                    .IsFixedLength();

                entity.Property(e => e.Id)
                    .HasColumnName("id")
                    .HasColumnType("bigint(20) unsigned")
                    .ValueGeneratedOnAdd();

                entity.Property(e => e.Name)
                    .IsRequired()
                    .HasColumnName("name")
                    .HasMaxLength(20)
                    .IsUnicode(false);
            });

            OnModelCreatingPartial(modelBuilder);
        }

        partial void OnModelCreatingPartial(ModelBuilder modelBuilder);
    }
}
